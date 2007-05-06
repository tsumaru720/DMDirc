/*
 * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package uk.org.ownage.dmdirc.actions;

/**
 * An action condition represents one condition within an action.
 * @author chris
 */
public class ActionCondition {
    
    /** The argument number that this action condition applies to. */
    private final int arg;
    
    /** The component that this action condition applies to. */
    private final ActionComponent component;
    
    /** The comparison that should be used for this condition. */
    private final ActionComparison comparison;
    
    /** The target of the comparison for this condition. */
    private final String target;
    
    /**
     * Creates a new instance of ActionCondition.
     * @param arg The argument number to be tested
     * @param component The component to be tested
     * @param comparison The comparison to be used
     * @param target The target of the comparison
     */
    public ActionCondition(final int arg, final ActionComponent component,
            final ActionComparison comparison, final String target) {
        this.arg = arg;
        this.component = component;
        this.comparison = comparison;
        this.target = target;
    }
    
    /**
     * Tests to see if this condition holds.
     * @param args The event arguments to be tested
     * @return True if the condition holds, false otherwise
     */
    public boolean test(final Object ... args) {
        final String thisTarget = ActionManager.substituteVars(target, args);
        return comparison.test(component.get(args[arg]), thisTarget);
    }
    
}
