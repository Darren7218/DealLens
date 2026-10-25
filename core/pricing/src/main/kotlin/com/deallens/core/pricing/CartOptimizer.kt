package com.deallens.core.pricing

import com.deallens.core.domain.Money
import com.deallens.core.domain.VoucherType
import java.math.BigDecimal
import java.math.RoundingMode

class CartOptimizer {

    fun optimize(request: CartOptimizationRequest): CartOptimizationResult {
        val items = request.items
        if (items.isEmpty()) {
            return CartOptimizationResult(
                splits = emptyList(),
                totalAmountDue = Money(0, items.firstOrNull()?.price?.currency ?: com.deallens.core.domain.CurrencyCode.MYR),
                totalDiscount = Money(0, items.firstOrNull()?.price?.currency ?: com.deallens.core.domain.CurrencyCode.MYR),
                explanation = "Empty cart",
                marginalEvaluations = emptyList()
            )
        }
        val currency = items.first().price.currency

        val partitions = generatePartitions(items)
        var bestResult: CartOptimizationResult? = null

        for (partition in partitions) {
            // For each order in the partition, find all valid subsets of vouchers
            val validVoucherSubsetsPerOrder = partition.map { orderItems ->
                getValidVoucherSubsets(orderItems, request.vouchers)
            }

            // Generate all combinations of voucher assignments across the partition
            val assignments = generateCombinations(validVoucherSubsetsPerOrder)

            for (assignment in assignments) {
                // Check global constraints: non-reusable vouchers can't be used more than once across all orders
                if (!isValidGlobalAssignment(assignment)) continue

                val splits = mutableListOf<OrderSplit>()
                var totalAmountDue = Money(0, currency)
                var totalDiscount = Money(0, currency)
                var isValid = true

                for (i in partition.indices) {
                    val orderItems = partition[i]
                    val orderVouchers = assignment[i]
                    val split = evaluateOrder(orderItems, orderVouchers, currency)
                    if (split == null) {
                        isValid = false
                        break
                    }
                    splits.add(split)
                    totalAmountDue += split.amountDue
                    totalDiscount += split.immediateDiscount
                }

                if (isValid) {
                    if (bestResult == null || totalAmountDue < bestResult.totalAmountDue) {
                        bestResult = CartOptimizationResult(
                            splits = splits,
                            totalAmountDue = totalAmountDue,
                            totalDiscount = totalDiscount,
                            explanation = if (partition.size > 1) "Splitting into ${partition.size} orders saves more money." else "Grouping all items together is optimal.",
                            marginalEvaluations = emptyList() // Calculated later
                        )
                    }
                }
            }
        }

        val finalResult = bestResult ?: CartOptimizationResult(
            splits = listOf(OrderSplit(items, emptyList(), calculateSubtotal(items, currency), Money(0, currency), calculateSubtotal(items, currency))),
            totalAmountDue = calculateSubtotal(items, currency),
            totalDiscount = Money(0, currency),
            explanation = "No valid voucher combinations found. Checked out as single order.",
            marginalEvaluations = emptyList()
        )

        // Calculate marginal evaluations
        val marginals = calculateMarginals(request, finalResult)
        return finalResult.copy(marginalEvaluations = marginals)
    }

    private fun calculateSubtotal(items: List<CartItem>, currency: com.deallens.core.domain.CurrencyCode): Money {
        return Money(items.sumOf { it.price.minorUnits * it.quantity }, currency)
    }

    private fun evaluateOrder(items: List<CartItem>, vouchers: List<CartVoucher>, currency: com.deallens.core.domain.CurrencyCode): OrderSplit? {
        val subtotal = calculateSubtotal(items, currency)
        var currentAmount = subtotal
        var totalDiscount = Money(0, currency)

        // Ensure stackability
        if (vouchers.size > 1 && vouchers.any { !it.isStackable }) {
            return null // Invalid subset
        }

        // Apply seller vouchers first, then platform vouchers
        val sortedVouchers = vouchers.sortedBy { if (it.sellerId != null) 0 else 1 }

        for (cv in sortedVouchers) {
            val v = cv.voucher
            val basisSubtotal = if (cv.sellerId != null) {
                calculateSubtotal(items.filter { it.sellerId == cv.sellerId }, currency)
            } else {
                subtotal
            }
            
            // Check minimum spend
            val minSpend = v.minimumSpend ?: Money(0, currency)
            if (basisSubtotal < minSpend) return null

            // Calculate discount
            val discountableAmount = if (cv.sellerId != null) {
                // Approximate remaining seller amount if we wanted to be strict, but for simplicity:
                basisSubtotal
            } else {
                currentAmount
            }

            var discountAmount = Money(0, currency)
            if (v.type == VoucherType.PERCENTAGE) {
                val discountDecimal = BigDecimal(discountableAmount.minorUnits)
                    .multiply(v.value)
                    .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
                discountAmount = Money(discountDecimal.toLong(), currency)
                
                val cap = v.cap
                if (cap != null && discountAmount > cap) {
                    discountAmount = cap
                }
            } else if (v.type == VoucherType.FIXED) {
                discountAmount = Money(v.value.multiply(BigDecimal("100")).toLong(), currency)
            }

            if (discountAmount > currentAmount) {
                discountAmount = currentAmount
            }

            currentAmount -= discountAmount
            totalDiscount += discountAmount
        }

        return OrderSplit(
            items = items,
            appliedVouchers = vouchers,
            subtotal = subtotal,
            immediateDiscount = totalDiscount,
            amountDue = currentAmount
        )
    }

    private fun getValidVoucherSubsets(items: List<CartItem>, vouchers: List<CartVoucher>): List<List<CartVoucher>> {
        val subsets = mutableListOf<List<CartVoucher>>()
        val n = vouchers.size
        for (i in 0 until (1 shl n)) {
            val subset = mutableListOf<CartVoucher>()
            for (j in 0 until n) {
                if ((i and (1 shl j)) != 0) {
                    subset.add(vouchers[j])
                }
            }
            if (subset.isEmpty()) {
                subsets.add(subset)
                continue
            }
            // Filter out non-stackable combinations early
            if (subset.size > 1 && subset.any { !it.isStackable }) continue
            
            // Filter if seller voucher doesn't match any item
            if (subset.any { cv -> cv.sellerId != null && items.none { it.sellerId == cv.sellerId } }) continue

            subsets.add(subset)
        }
        return subsets
    }

    private fun isValidGlobalAssignment(assignment: List<List<CartVoucher>>): Boolean {
        val usedCounts = mutableMapOf<String, Int>()
        for (orderVouchers in assignment) {
            for (cv in orderVouchers) {
                usedCounts[cv.voucher.id] = usedCounts.getOrDefault(cv.voucher.id, 0) + 1
            }
        }
        for (orderVouchers in assignment) {
            for (cv in orderVouchers) {
                if (!cv.isReusable && usedCounts[cv.voucher.id]!! > 1) {
                    return false
                }
            }
        }
        return true
    }

    private fun <T> generatePartitions(items: List<T>): List<List<List<T>>> {
        if (items.isEmpty()) return emptyList()
        val result = mutableListOf<List<List<T>>>()
        generatePartitionsHelper(items, 0, mutableListOf(), result)
        return result
    }

    private fun <T> generatePartitionsHelper(items: List<T>, index: Int, currentPartition: MutableList<MutableList<T>>, result: MutableList<List<List<T>>>) {
        if (index == items.size) {
            result.add(currentPartition.map { it.toList() })
            return
        }
        val item = items[index]
        for (i in currentPartition.indices) {
            currentPartition[i].add(item)
            generatePartitionsHelper(items, index + 1, currentPartition, result)
            currentPartition[i].removeAt(currentPartition[i].size - 1)
        }
        currentPartition.add(mutableListOf(item))
        generatePartitionsHelper(items, index + 1, currentPartition, result)
        currentPartition.removeAt(currentPartition.size - 1)
    }

    private fun <T> generateCombinations(lists: List<List<T>>): List<List<T>> {
        if (lists.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<T>>()
        generateCombinationsHelper(lists, 0, mutableListOf(), result)
        return result
    }

    private fun <T> generateCombinationsHelper(lists: List<List<T>>, index: Int, current: MutableList<T>, result: MutableList<List<T>>) {
        if (index == lists.size) {
            result.add(current.toList())
            return
        }
        for (item in lists[index]) {
            current.add(item)
            generateCombinationsHelper(lists, index + 1, current, result)
            current.removeAt(current.size - 1)
        }
    }

    private fun calculateMarginals(request: CartOptimizationRequest, optimalResult: CartOptimizationResult): List<MarginalEvaluation> {
        val marginals = mutableListOf<MarginalEvaluation>()
        val currency = request.items.firstOrNull()?.price?.currency ?: com.deallens.core.domain.CurrencyCode.MYR
        
        for (item in request.items) {
            val itemsWithout = request.items.filter { it.id != item.id }
            if (itemsWithout.isEmpty()) continue
            
            val requestWithout = request.copy(items = itemsWithout)
            val resultWithout = optimize(requestWithout)
            
            val marginalCash = optimalResult.totalAmountDue - resultWithout.totalAmountDue
            val marginalDiscount = optimalResult.totalDiscount - resultWithout.totalDiscount
            
            val thresholdMsg = if (marginalDiscount > Money(0, currency)) {
                "Adding this item unlocked an extra ${marginalDiscount.format()} discount. You spent ${marginalCash.format()} to get it."
            } else if (marginalDiscount < Money(0, currency)) {
                "Adding this item caused a loss of ${Money(-marginalDiscount.minorUnits, currency).format()} discount."
            } else {
                "Adding this item unlocked no extra discount."
            }
            
            val worthItMsg = if (marginalCash < item.price) {
                "Worth it: you paid less than the item's price (${item.price.format()})."
            } else {
                "Not strictly worth it for discount purposes alone."
            }

            marginals.add(MarginalEvaluation(
                item = item,
                marginalCashRequired = marginalCash,
                marginalDiscountGained = marginalDiscount,
                explanation = "$thresholdMsg $worthItMsg"
            ))
        }
        return marginals
    }

    private fun Money.format(): String {
        val bd = BigDecimal(this.minorUnits).divide(BigDecimal("100"))
        return "${this.currency.name} $bd"
    }
}
