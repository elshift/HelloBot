package org.elshift.modules.sakugabooru;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SakugabooruPost {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("tags")
    @Expose
    private String tags;
    @SerializedName("created_at")
    @Expose
    private Integer createdAt;
    @SerializedName("updated_at")
    @Expose
    private Integer updatedAt;
    @SerializedName("creator_id")
    @Expose
    private Integer creatorId;
    @SerializedName("approver_id")
    @Expose
    private Integer approverId;
    @SerializedName("author")
    @Expose
    private String author;
    @SerializedName("change")
    @Expose
    private Integer change;
    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("score")
    @Expose
    private Integer score;
    @SerializedName("md5")
    @Expose
    private String md5;
    @SerializedName("file_size")
    @Expose
    private Integer fileSize;
    @SerializedName("file_ext")
    @Expose
    private String fileExt;
    @SerializedName("file_url")
    @Expose
    private String fileUrl;
    @SerializedName("is_shown_in_index")
    @Expose
    private Boolean isShownInIndex;
    @SerializedName("preview_url")
    @Expose
    private String previewUrl;
    @SerializedName("preview_width")
    @Expose
    private Integer previewWidth;
    @SerializedName("preview_height")
    @Expose
    private Integer previewHeight;
    @SerializedName("actual_preview_width")
    @Expose
    private Integer actualPreviewWidth;
    @SerializedName("actual_preview_height")
    @Expose
    private Integer actualPreviewHeight;
    @SerializedName("sample_url")
    @Expose
    private String sampleUrl;
    @SerializedName("sample_width")
    @Expose
    private Integer sampleWidth;
    @SerializedName("sample_height")
    @Expose
    private Integer sampleHeight;
    @SerializedName("sample_file_size")
    @Expose
    private Integer sampleFileSize;
    @SerializedName("jpeg_url")
    @Expose
    private String jpegUrl;
    @SerializedName("jpeg_width")
    @Expose
    private Integer jpegWidth;
    @SerializedName("jpeg_height")
    @Expose
    private Integer jpegHeight;
    @SerializedName("jpeg_file_size")
    @Expose
    private Integer jpegFileSize;
    @SerializedName("rating")
    @Expose
    private String rating;
    @SerializedName("is_rating_locked")
    @Expose
    private Boolean isRatingLocked;
    @SerializedName("has_children")
    @Expose
    private Boolean hasChildren;
    @SerializedName("parent_id")
    @Expose
    private Object parentId;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("is_pending")
    @Expose
    private Boolean isPending;
    @SerializedName("width")
    @Expose
    private Integer width;
    @SerializedName("height")
    @Expose
    private Integer height;
    @SerializedName("is_held")
    @Expose
    private Boolean isHeld;
    @SerializedName("frames_pending_string")
    @Expose
    private String framesPendingString;
    @SerializedName("frames_pending")
    @Expose
    private List<Object> framesPending = null;
    @SerializedName("frames_string")
    @Expose
    private String framesString;
    @SerializedName("frames")
    @Expose
    private List<Object> frames = null;
    @SerializedName("is_note_locked")
    @Expose
    private Boolean isNoteLocked;
    @SerializedName("last_noted_at")
    @Expose
    private Integer lastNotedAt;
    @SerializedName("last_commented_at")
    @Expose
    private Integer lastCommentedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Integer createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Integer updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getApproverId() {
        return approverId;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getChange() {
        return change;
    }

    public void setChange(Integer change) {
        this.change = change;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Boolean getIsShownInIndex() {
        return isShownInIndex;
    }

    public void setIsShownInIndex(Boolean isShownInIndex) {
        this.isShownInIndex = isShownInIndex;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public Integer getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(Integer previewWidth) {
        this.previewWidth = previewWidth;
    }

    public Integer getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(Integer previewHeight) {
        this.previewHeight = previewHeight;
    }

    public Integer getActualPreviewWidth() {
        return actualPreviewWidth;
    }

    public void setActualPreviewWidth(Integer actualPreviewWidth) {
        this.actualPreviewWidth = actualPreviewWidth;
    }

    public Integer getActualPreviewHeight() {
        return actualPreviewHeight;
    }

    public void setActualPreviewHeight(Integer actualPreviewHeight) {
        this.actualPreviewHeight = actualPreviewHeight;
    }

    public String getSampleUrl() {
        return sampleUrl;
    }

    public void setSampleUrl(String sampleUrl) {
        this.sampleUrl = sampleUrl;
    }

    public Integer getSampleWidth() {
        return sampleWidth;
    }

    public void setSampleWidth(Integer sampleWidth) {
        this.sampleWidth = sampleWidth;
    }

    public Integer getSampleHeight() {
        return sampleHeight;
    }

    public void setSampleHeight(Integer sampleHeight) {
        this.sampleHeight = sampleHeight;
    }

    public Integer getSampleFileSize() {
        return sampleFileSize;
    }

    public void setSampleFileSize(Integer sampleFileSize) {
        this.sampleFileSize = sampleFileSize;
    }

    public String getJpegUrl() {
        return jpegUrl;
    }

    public void setJpegUrl(String jpegUrl) {
        this.jpegUrl = jpegUrl;
    }

    public Integer getJpegWidth() {
        return jpegWidth;
    }

    public void setJpegWidth(Integer jpegWidth) {
        this.jpegWidth = jpegWidth;
    }

    public Integer getJpegHeight() {
        return jpegHeight;
    }

    public void setJpegHeight(Integer jpegHeight) {
        this.jpegHeight = jpegHeight;
    }

    public Integer getJpegFileSize() {
        return jpegFileSize;
    }

    public void setJpegFileSize(Integer jpegFileSize) {
        this.jpegFileSize = jpegFileSize;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Boolean getIsRatingLocked() {
        return isRatingLocked;
    }

    public void setIsRatingLocked(Boolean isRatingLocked) {
        this.isRatingLocked = isRatingLocked;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Object getParentId() {
        return parentId;
    }

    public void setParentId(Object parentId) {
        this.parentId = parentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsPending() {
        return isPending;
    }

    public void setIsPending(Boolean isPending) {
        this.isPending = isPending;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Boolean getIsHeld() {
        return isHeld;
    }

    public void setIsHeld(Boolean isHeld) {
        this.isHeld = isHeld;
    }

    public String getFramesPendingString() {
        return framesPendingString;
    }

    public void setFramesPendingString(String framesPendingString) {
        this.framesPendingString = framesPendingString;
    }

    public List<Object> getFramesPending() {
        return framesPending;
    }

    public void setFramesPending(List<Object> framesPending) {
        this.framesPending = framesPending;
    }

    public String getFramesString() {
        return framesString;
    }

    public void setFramesString(String framesString) {
        this.framesString = framesString;
    }

    public List<Object> getFrames() {
        return frames;
    }

    public void setFrames(List<Object> frames) {
        this.frames = frames;
    }

    public Boolean getIsNoteLocked() {
        return isNoteLocked;
    }

    public void setIsNoteLocked(Boolean isNoteLocked) {
        this.isNoteLocked = isNoteLocked;
    }

    public Integer getLastNotedAt() {
        return lastNotedAt;
    }

    public void setLastNotedAt(Integer lastNotedAt) {
        this.lastNotedAt = lastNotedAt;
    }

    public Integer getLastCommentedAt() {
        return lastCommentedAt;
    }

    public void setLastCommentedAt(Integer lastCommentedAt) {
        this.lastCommentedAt = lastCommentedAt;
    }

}