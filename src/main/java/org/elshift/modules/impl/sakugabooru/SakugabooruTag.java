package org.elshift.modules.impl.sakugabooru;

import com.google.gson.annotations.SerializedName;
import org.elshift.db.annotations.SqlPrimaryKey;

public class SakugabooruTag {
    public enum Order {
        DATE,
        COUNT,
        TIME;

        public final String urlValue;

        Order() {
            this.urlValue = this.name().toLowerCase();
        }
    }

    public enum Type {
        GENERAL(0),
        ARTIST(1),
        COPYRIGHT(3),
        CHARACTER(4);

        public final int intValue;
        public final String urlValue;

        Type(int value) {
            this.intValue = value;
            this.urlValue = this.name().toLowerCase();
        }
    }

    @SerializedName("id")
    @SqlPrimaryKey
    public Integer id;
    @SerializedName("name")
    public String name;
    @SerializedName("count")
    public Integer count;
    @SerializedName("type")
    public Integer type;
    @SerializedName("ambiguous")
    public Boolean ambiguous;

}