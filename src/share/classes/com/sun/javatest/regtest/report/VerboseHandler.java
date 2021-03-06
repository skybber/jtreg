/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javatest.regtest.report;

import java.io.PrintWriter;

import com.sun.javatest.Harness;
import com.sun.javatest.Status;
import com.sun.javatest.TestDescription;
import com.sun.javatest.TestResult;

// TODO: I18N

public class VerboseHandler {

    public VerboseHandler(Verbose verbose, PrintWriter out, PrintWriter err) {
        this.verbose = verbose;
        this.out = out;
        this.err = err;
    }

    public void register(Harness h) {
        h.addObserver(new BasicObserver() {
            @Override
            public synchronized void startingTest(TestResult tr) {
                VerboseHandler.this.startingTest(tr);
            }
            @Override
            public synchronized void finishedTest(TestResult tr) {
                VerboseHandler.this.finishedTest(tr);
            }
        });
    }

    private void startingTest(TestResult tr) {
        if (verbose.isDefault()) {
            try {
                TestDescription td = tr.getDescription();
                out.println("runner starting test: "
                        + td.getRootRelativeURL());
            } catch(TestResult.Fault e) {
                e.printStackTrace(System.err);
            }
        }
    } // starting()

    private void finishedTest(TestResult tr) {
        Verbose.Mode m;
        switch (tr.getStatus().getType()) {
            case Status.PASSED:
                m = verbose.passMode;
                break;
            case Status.FAILED:
                m = verbose.failMode;
                break;
            case Status.ERROR:
                m = verbose.errorMode;
                break;
            default:
                m = Verbose.Mode.NONE;
        }

        switch (m) {
            case NONE:
                break;

            case DEFAULT:
                try {
                    TestDescription td = tr.getDescription();
                    if (verbose.time)
                        printElapsedTimes(tr);
                    out.println("runner finished test: "
                            + td.getRootRelativeURL());
                    out.println(tr.getStatus());
                } catch (TestResult.Fault e) {
                    e.printStackTrace(System.err);
                }
                break;

            case SUMMARY:
                printSummary(tr, verbose.time);
                break;

            case BRIEF:
                printBriefOutput(tr, verbose.time);
                break;

            case FULL:
                printFullOutput(tr);
                break;
        }
    } // finished()

    /**
     * Print out one line per test indicating the status category for the
     * named test.
     *
     * @param tr TestResult containing all recorded information from a
     *         test's run.
     */
    private void printSummary(TestResult tr, boolean times) {
        try {
            TestDescription td = tr.getDescription();
            String msg;

            if (tr.getStatus().isPassed())
                msg = "Passed: ";
            else if (tr.getStatus().isFailed())
                msg = "FAILED: ";
            else if (tr.getStatus().isError())
                msg = "Error:  ";
            else
                msg = "Unexpected status: ";
            msg += td.getRootRelativeURL();
            out.println(msg);

            if (times)
                printElapsedTimes(tr);

        } catch (TestResult.Fault e) {
            e.printStackTrace(System.err);
        }
    } // printSummary()

    /**
     * Print out two lines per test indicating the name of the test and the
     * final status.
     *
     * @param tr TestResult containing all recorded information from a
     *         test's run.
     */
    private void printBriefOutput(TestResult tr, boolean times) {
        if (!doneSeparator) {
            out.println(VERBOSE_TEST_SEP);
            doneSeparator = true;
        }

        try {
            TestDescription td = tr.getDescription();
            out.println("TEST: " + td.getRootRelativeURL());

            if (times)
                printElapsedTimes(tr);

            out.println("TEST RESULT: " + tr.getStatus());
        } catch (TestResult.Fault e) {
            e.printStackTrace(System.err);
        }

        out.println(VERBOSE_TEST_SEP);
    } // printBriefOutput()

    /**
     * Print out full information for the test.  This includes the reason
     * for running each action, the action's output streams, the final
     * status, etc.   This is meant to encompass all of the information
     * from the .jtr file that the average user would find useful.
     *
     * @param tr TestResult containing all recorded information from a
     *         test's run.
     */
    private void printFullOutput(TestResult tr) {
        if (!doneSeparator) {
            out.println(VERBOSE_TEST_SEP);
            doneSeparator = true;
        }

        try {
            TestDescription td = tr.getDescription();
            out.println("TEST: " + td.getRootRelativeURL());
            String testJDK = getTestJDK(tr);
            if (testJDK != null) {
                out.println("TEST JDK: " + testJDK);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < tr.getSectionCount(); i++) {
                TestResult.Section section = tr.getSection(i);
                sb.append(LINESEP);
                sb.append("ACTION: ").append(section.getTitle());
                sb.append(" -- ").append(section.getStatus()).append(LINESEP);
                sb.append("REASON: ").append(getReason(section)).append(LINESEP);
                sb.append("TIME:   ").append(getElapsedTime(section));
                sb.append(" seconds").append(LINESEP);

                String[] outputNames = section.getOutputNames();
                for (String name : outputNames) {
                    String output = section.getOutput(name);
                    switch (name) {
                        case "System.out":
                            sb.append("STDOUT:").append(LINESEP).append(output);
                            break;
                        case "System.err":
                            sb.append("STDERR:").append(LINESEP).append(output);
                            break;
                        default:
                            sb.append(name).append(":").append(LINESEP).append(output);
                            break;
                    }
                }
            }
            out.println(sb.toString());

            out.println("TEST RESULT: " + tr.getStatus());
            out.println(VERBOSE_TEST_SEP);
        } catch (TestResult.Fault e) {
            e.printStackTrace(System.err);
        }
    } // printFullOutput()

    /**
     * Print out one line per test indicating the status category for the
     * named test and the elapsed time per action in the test.
     *
     * @param tr TestResult containing all recorded information from a
     *         test's run.
     */
    private void printElapsedTimes(TestResult tr) {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 1; i < tr.getSectionCount(); i++) {
                TestResult.Section section = tr.getSection(i);
                sb.append("  ").append(section.getTitle()).append(": ");
                sb.append(getElapsedTime(section)).append(" seconds").append(LINESEP);
            }
            out.print(sb.toString());
        } catch (TestResult.ReloadFault f) {
            f.printStackTrace(System.err);
        }
    } // printElapsedTimes()

    /**
     * Find the reason that the action was run.  This method takes
     * advantage of the fact that the reason string begins with
     * {@code reason: }, ends with a line separator.
     *
     * @param section The recorded information for a single action.
     * @return The reason string without the beginning
     * {@code reason: }
     * string.
     */
    private String getReason(TestResult.Section section) {
        String msg = section.getOutput(TestResult.MESSAGE_OUTPUT_NAME);
        int posStart = msg.indexOf("reason: ") + "reason: ".length();
        int posEnd   = msg.indexOf(LINESEP, posStart);
        return msg.substring(posStart, posEnd);
    } // getReason()

    /**
     * Find the elapsed time for the action.  This method takes advantage
     * of the fact that the string containing the elapsed time begins with
     * {@code elapsed time (seconds): }, and ends with a line
     * separator.
     *
     * @param section The recorded information for a single action.
     * @return The elapsed time without the beginning {@code elapsed time
     * (seconds): } as a string.
     */
    private String getElapsedTime(TestResult.Section section) {
        String msg = section.getOutput(TestResult.MESSAGE_OUTPUT_NAME);
        int posStart = msg.indexOf("elapsed time (seconds): ") +
                "elapsed time (seconds): ".length();
        int posEnd = msg.indexOf(LINESEP, posStart);
        return msg.substring(posStart, posEnd);
    } // getElapsedTime()


    /**
     * Find the JDK under test.
     *
     * @param tr The test result where all sections are stored.
     * @return The string indicating the JDK under test, or null if not found
     */
    private String getTestJDK(TestResult tr) {
        try {
            return tr.getProperty("testJDK");
        } catch (TestResult.Fault f) {
            f.printStackTrace(System.err);
        }
        return null;
    } // getTestJDK()

    //----------member variables-------------------------------------------

    private static final String VERBOSE_TEST_SEP = "--------------------------------------------------";
    private static final String LINESEP = System.getProperty("line.separator");

    private final Verbose verbose;
    private final PrintWriter out;
    private final PrintWriter err;
    private boolean doneSeparator;
}
