package com.techtown.naverm;

public class Place {
    public String PJT_NAME;
    public String PJT_DATE;
    public String SITE_ADDR;
    public double LAT;
    public double LNG;


    public double getLAT() {
        return LAT;
    }

    public double getLNG(){
        return LNG;
    }

    public String getPJT_NAME(){
        return PJT_NAME;
    }

    public String getPJT_DATE(){
        return PJT_DATE;
    }

    public String getSITE_ADDR(){
        return SITE_ADDR;
    }
}

