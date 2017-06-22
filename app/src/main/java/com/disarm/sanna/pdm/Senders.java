package com.disarm.sanna.pdm;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by arka on 15/9/16.
 */
public class Senders implements Parcelable {
    String number;
    String name;
    ArrayList<File> allFiles;
    ArrayList<File> textFiles;
    ArrayList<File> imageFiles;
    ArrayList<File> videoFiles;
    ArrayList<File> recordingFiles;
    ArrayList<File> smsFiles;

    public Senders(String number, String name) {
        this.number = number;
        this.name = name;
        allFiles = new ArrayList<>();
        textFiles = new ArrayList<>();
        imageFiles = new ArrayList<>();
        videoFiles = new ArrayList<>();
        recordingFiles = new ArrayList<>();
        smsFiles = new ArrayList<>();
    }

    public void addFile(File file) {
        this.allFiles.add(file);
    }

    public void addText(File file) {
        this.textFiles.add(file);
    }

    public void addImage(File file) {
        this.imageFiles.add(file);
    }

    public void addVideo(File file) {
        this.videoFiles.add(file);
    }

    public void addRecording(File file) {
        this.recordingFiles.add(file);
    }

    public void addSms(File file) {
        this.smsFiles.add(file);
    }

    public String getNumber() {
        return this.number;
    }

    public ArrayList<File> getAllFiles() {
        return this.allFiles;
    }

    public ArrayList<File> getTextFiles() {
        return this.textFiles;
    }

    public ArrayList<File> getImageFiles() {
        return this.imageFiles;
    }

    public ArrayList<File> getVideoFiles() {
        return this.videoFiles;
    }

    public ArrayList<File> getRecordingFiles() {
        return this.recordingFiles;
    }

    public ArrayList<File> getSmsFiles() {
        return this.smsFiles;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(number);
        parcel.writeList(allFiles);
        parcel.writeList(imageFiles);
        parcel.writeList(videoFiles);
        parcel.writeList(recordingFiles);
        parcel.writeList(textFiles);
        parcel.writeList(smsFiles);
    }

    public static final Parcelable.Creator<Senders> CREATOR
            = new Parcelable.Creator<Senders>() {
        @Override
        public Senders createFromParcel(Parcel parcel) {
            return new Senders(parcel);
        }

        @Override
        public Senders[] newArray(int i) {
            return new Senders[0];
        }
    };

    private Senders(Parcel parcel) {
        number = parcel.readString();
        allFiles = parcel.readArrayList(null);
        imageFiles = parcel.readArrayList(null);
        videoFiles = parcel.readArrayList(null);
        recordingFiles = parcel.readArrayList(null);
        textFiles = parcel.readArrayList(null);
        smsFiles = parcel.readArrayList(null);
    }
}