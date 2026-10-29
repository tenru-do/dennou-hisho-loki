package com.example.rokidkeyboardbridge;
/** A small comfortable gap only. Does not interpret broad pose words or quoted instructions. */
final class RestLegRelaxIntent {
 static final String COMMAND="足を少し開いて";
 static final String REPLY="寝姿のまま脚を少し離して、楽に休みます。";
 static boolean request(String text){String t=OffDutyPoseIntent.clean(text);if(OffDutyPoseIntent.excluded(t))return false;boolean slight=t.contains("少し")||t.contains("ちょっと");return slight&&t.matches("(?:(?:寝たまま|寝そべったまま)[、, ]*)?(?:(?:少し|ちょっと)[、, ]*)?(?:脚|足)を(?:(?:少し|ちょっと)[、, ]*)?(?:離して|開いて|開かせて)(?:みて|ください|ね|よ)?[。！!\\s]*");}
}
