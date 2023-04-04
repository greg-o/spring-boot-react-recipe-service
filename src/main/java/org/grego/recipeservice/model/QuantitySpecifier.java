/**
 * Recipe Service.
 * Copyright: none
 *
 * @author Greg-O
 */
package org.grego.recipeservice.model;


/**
 * QuantitySpecifier is to specify a quantity.
 */
public enum QuantitySpecifier {
    /**
     * Unspecified quantity.
     */
    Unspecified,
    /**
     * Specify count for the quantity.
     */
    Count,
    /**
     * Specify dash for the quantity.
     */
    Dash,
    /**
     * Specify drop for the quantity.
     */
    Drop,
    /**
     * Specify teaspoon for the quantity.
     */
    Teaspoon,
    /**
     * Specify tablespoon for the quantity.
     */
    Tablespoon,
    /**
     * Specify fluid ounces for the quantity.
     */
    FluidOunce,
    /**
     * Specify cups for the quantity.
     */
    Cup,
    /**
     * Specify pints for the quantity.
     */
    Pint,
    /**
     * Specify quarts for the quantity.
     */
    Quart,
    /**
     * Specify gallons for the quantity.
     */
    Gallon,
    /**
     * Specify a pinch for the quantity.
     */
    Pinch,
    /**
     * Specify milliliters for the quantity.
     */
    Milliliter,
    /**
     * Specify liter for the quantity.
     */
    Liter,
    /**
     * Specify ounces for the quantity.
     */
    Ounce,
    /**
     * Specify pounds for the quantity.
     */
    Pound,
    /**
     * Specify milligrams for the quantity.
     */
    Milligram,
    /**
     * Specify grams for the quantity.
     */
    Gram,
    /**
     * Specify kilograms for the quantity.
     */
    Kilogram
}
