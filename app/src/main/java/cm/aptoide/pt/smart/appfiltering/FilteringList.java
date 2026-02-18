package cm.aptoide.pt.smart.appfiltering;


import java.util.ArrayList;
import java.util.List;

class FilteringList {
    public static final List<AppToRemove> removingList;

    static  {
        removingList = new ArrayList<>();
        removingList.add(new AppToRemove("com.smarttechapp.SDC", "2.1.1011.0"));
    }


}
