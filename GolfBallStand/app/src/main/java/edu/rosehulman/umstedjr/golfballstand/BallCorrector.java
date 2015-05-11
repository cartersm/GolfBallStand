package edu.rosehulman.umstedjr.golfballstand;

import java.util.Random;

@SuppressWarnings("UnqualifiedStaticUsage")
public class BallCorrector {
    public static final Random RAND = new Random();
    public enum BallColor {
        NONE,
        RED,
        GREEN,
        BLUE,
        YELLOW,
        WHITE,
        BLACK
    }

    public static BallColor[] convertToBallColors(String[] ballColorStrings) {
        BallColor[] ballColors = new BallColor[3];
        for (int i = 0; i < ballColorStrings.length; i++) {
            String ballColor = ballColorStrings[i];
            ballColors[i] = getColorForString(ballColor);
        }
        return ballColors;
    }

    public static BallColor getColorForString(String ballColor) {
        for (BallColor c : BallColor.values()) {
            if (c.name().equalsIgnoreCase(ballColor)) {
                return c;
            }
        }
        return BallColor.NONE;
    }

    public static boolean isNone(BallColor ballColor) {
        return ballColor.name().equals(BallColor.NONE.name());
    }

    public static boolean isColors(BallColor ballColor, BallColor color1, BallColor color2) {
        return ballColor.name().equals(color1.name()) || ballColor.name().equals(color2.name());
    }

    public static boolean isColor(BallColor ballColor, BallColor color) {
        return ballColor.name().equals(color.name());
    }

    public static void checkYB(BallColor[] ballColors) {
        if (isColor(ballColors[0], BallColor.YELLOW)) {
            if (isColor(ballColors[1], BallColor.BLUE)) {
                ballColors[1] = BallColor.GREEN;
            } else if (isColor(ballColors[2], BallColor.BLUE)) {
                ballColors[2] = BallColor.GREEN;
            } else if (isColor(ballColors[1], BallColor.YELLOW)) {
                ballColors[1] = BallColor.WHITE;
            } else if (isColor(ballColors[2], BallColor.YELLOW)) {
                ballColors[2] = BallColor.WHITE;
            }
        } else if (isColor(ballColors[1], BallColor.YELLOW)) {
            if (isColor(ballColors[0], BallColor.BLUE)) {
                ballColors[0] = BallColor.GREEN;
            } else if (isColor(ballColors[2], BallColor.BLUE)) {
                ballColors[2] = BallColor.GREEN;
            } else if (isColor(ballColors[0], BallColor.YELLOW)) {
                ballColors[0] = BallColor.WHITE;
            } else if (isColor(ballColors[2], BallColor.YELLOW)) {
                ballColors[2] = BallColor.WHITE;
            }
        } else if (isColor(ballColors[2], BallColor.YELLOW)) {
            if (isColor(ballColors[1], BallColor.BLUE)) {
                ballColors[1] = BallColor.GREEN;
            } else if (isColor(ballColors[0], BallColor.BLUE)) {
                ballColors[0] = BallColor.GREEN;
            } else if (isColor(ballColors[1], BallColor.YELLOW)) {
                ballColors[1] = BallColor.WHITE;
            } else if (isColor(ballColors[0], BallColor.YELLOW)) {
                ballColors[0] = BallColor.WHITE;
            }
        }
    }

    public static void checkRG(BallColor[] ballColors) {
        if (isColor(ballColors[0], BallColor.RED)) {
            if (isColor(ballColors[1], BallColor.GREEN)) {
                ballColors[1] = BallColor.BLUE;
            } else if (isColor(ballColors[2], BallColor.GREEN)) {
                ballColors[2] = BallColor.BLUE;
            }
        } else if (isColor(ballColors[1], BallColor.RED)) {
            if (isColor(ballColors[0], BallColor.GREEN)) {
                ballColors[0] = BallColor.BLUE;
            } else if (isColor(ballColors[2], BallColor.GREEN)) {
                ballColors[2] = BallColor.BLUE;
            }
        } else if (isColor(ballColors[2], BallColor.RED)) {
            if (isColor(ballColors[1], BallColor.GREEN)) {
                ballColors[1] = BallColor.BLUE;
            } else if (isColor(ballColors[0], BallColor.GREEN)) {
                ballColors[0] = BallColor.BLUE;
            }
        }
    }

    public static void checkBW(BallColor[] ballColors) {
        if (isColor(ballColors[0], BallColor.WHITE)) {
            if (isColor(ballColors[1], BallColor.BLACK)) {
                ballColors[1] = BallColor.GREEN;
            } else if (isColor(ballColors[2], BallColor.BLACK)) {
                ballColors[2] = BallColor.GREEN;
            }
        } else if (isColor(ballColors[1], BallColor.WHITE)) {
            if (isColor(ballColors[0], BallColor.BLACK)) {
                ballColors[0] = BallColor.GREEN;
            } else if (isColor(ballColors[2], BallColor.BLACK)) {
                ballColors[2] = BallColor.GREEN;
            }
        } else if (isColor(ballColors[2], BallColor.WHITE)) {
            if (isColor(ballColors[1], BallColor.BLACK)) {
                ballColors[1] = BallColor.GREEN;
            } else if (isColor(ballColors[0], BallColor.BLACK)) {
                ballColors[0] = BallColor.GREEN;
            }
        }
    }

    public static void checkNoneToRG(BallColor[] ballColors) {
        if (isNone(ballColors[0]) &&
                (isColors(ballColors[1], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[2], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[2], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[1], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[0] = (RAND.nextBoolean()) ? BallColor.RED : BallColor.GREEN;
        } else if (isNone(ballColors[1]) &&
                (isColors(ballColors[0], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[2], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[2], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[0], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[1] = (RAND.nextBoolean()) ? BallColor.RED : BallColor.GREEN;
        } else if (isNone(ballColors[2]) &&
                (isColors(ballColors[0], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[1], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[1], BallColor.YELLOW, BallColor.BLUE)
                        && isColors(ballColors[0], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[2] = (RAND.nextBoolean()) ? BallColor.RED : BallColor.GREEN;
        }
    }

    public static void checkNoneToYB(BallColor[] ballColors) {
        if (isNone(ballColors[0]) &&
                (isColors(ballColors[1], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[2], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[2], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[1], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[0] = (RAND.nextBoolean()) ? BallColor.YELLOW : BallColor.BLUE;
        } else if (isNone(ballColors[1]) &&
                (isColors(ballColors[0], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[2], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[2], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[0], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[1] = (RAND.nextBoolean()) ? BallColor.YELLOW : BallColor.BLUE;
        } else if (isNone(ballColors[2]) &&
                (isColors(ballColors[0], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[1], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[1], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[0], BallColor.WHITE, BallColor.BLACK))) {
            ballColors[1] = (RAND.nextBoolean()) ? BallColor.YELLOW : BallColor.BLUE;
        }
    }

    public static void checkNoneToBW(BallColor[] ballColors) {
        if (isNone(ballColors[0]) &&
                (isColors(ballColors[1], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[2], BallColor.YELLOW, BallColor.BLUE)
                || isColors(ballColors[2], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[1], BallColor.YELLOW, BallColor.BLUE))) {
            ballColors[0] = (RAND.nextBoolean()) ? BallColor.WHITE : BallColor.BLACK;
        } else if (isNone(ballColors[1]) &&
                (isColors(ballColors[0], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[2], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[2], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[0], BallColor.YELLOW, BallColor.BLUE))) {
            ballColors[1] = (RAND.nextBoolean()) ? BallColor.WHITE : BallColor.BLACK;
        } else if (isNone(ballColors[2]) &&
                (isColors(ballColors[0], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[1], BallColor.WHITE, BallColor.BLACK)
                || isColors(ballColors[1], BallColor.RED, BallColor.GREEN)
                        && isColors(ballColors[0], BallColor.YELLOW, BallColor.BLUE))) {
            ballColors[1] = (RAND.nextBoolean()) ? BallColor.WHITE : BallColor.BLACK;
        }
    }
}
