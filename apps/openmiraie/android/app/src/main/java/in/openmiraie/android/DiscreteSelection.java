package in.openmiraie.android;

/** Release-only draft used by the two native discrete sliders. */
final class DiscreteSelection {
    final int count,observed;private Integer draft;
    DiscreteSelection(int count,int observed){this.count=count;this.observed=observed;}
    int current(){return draft==null?observed:draft;}
    void select(int index){if(count>0)draft=Math.max(0,Math.min(count-1,index));}
    Integer consume(){Integer result=draft;draft=null;return result==null||result==observed?null:result;}
    void cancel(){draft=null;}
    boolean editing(){return draft!=null;}
}
