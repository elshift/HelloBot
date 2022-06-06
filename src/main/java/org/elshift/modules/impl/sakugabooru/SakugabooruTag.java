package org.elshift.modules.impl.sakugabooru;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("count")
    @Expose
    private Integer count;
    @SerializedName("type")
    @Expose
    private Integer type;
    @SerializedName("ambiguous")
    @Expose
    private Boolean ambiguous;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Boolean getAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(Boolean ambiguous) {
        this.ambiguous = ambiguous;
    }

}