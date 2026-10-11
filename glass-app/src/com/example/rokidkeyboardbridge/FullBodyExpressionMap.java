package com.example.rokidkeyboardbridge;

/** Maps portrait semantic cues to visually reviewed full-body artwork.
 * Keep this independent of the portrait selector: artwork IDs are not emotion IDs.
 */
final class FullBodyExpressionMap {
    static int pose(int expression) {
        int id=Math.max(0,Math.min(167,expression));
        switch(id) {
            case 2: return 10; // Search: hand at chin, rather than crossed arms.
            case 6: return 36; // Error/apology: worried clasped hands.
            case 9: return 28; // Fatigue: lowered head, hand at temple.
            case 10: return 3; // Sadness: downcast gaze, hand at chest.
            case 14: return 0; // Question: inviting open palm.
            case 15: return 49; // Anger: tense stance and clenched hands.
            case 37: return 11; // Impact: startled face and raised hands.
            case 39: return 10; // Searching memory: thoughtful chin gesture.
            case 43: return 3; // Supportive concern.
            case 44: return 18; // Mild surprise, open hands.
            case 46: return 27; // Strong surprise, both hands raised.
            case 49: return 20; // Enduring tension, folded arms.
            case 53: return 5; // Achievement, confident presentation.
            case 55: return 2; // Refusal, firm crossed arms; no sunglasses.
            case 56: return 20;
            case 57: return 29;
            case 59: return 102; // Strong distress, tense raised hands.
            case 66: return 63; // Breath: chest gesture, rather than presentation.
            case 75: return 74; // Recovery, rather than presentation.
            case 78: return 80; // Tension, rather than presentation.
            case 94: return 103; // Crying: hands near tearful face.
            case 96: return 111;
            case 97: return 109;
            case 98: return 105; // Sorrow, lowered head.
            case 102: return 104; // Resistance, protective folded arms.
            case 115: return 108; // Distress, rather than explaining gesture.
            case 117: return 116;
            case 162: return 156; // Strong reaction, rather than presentation.
            default: return id;
        }
    }
}
