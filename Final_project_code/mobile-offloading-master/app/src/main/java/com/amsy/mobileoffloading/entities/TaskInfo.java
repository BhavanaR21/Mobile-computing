package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class TaskInfo implements Serializable {
    private int partIdx;
    private int[] rows;
    private int[] cols;

    private int workCapacity;

    public int getPartIdx() {
        return partIdx;
    }

    public void setPartIdx(int partIdx) {
        this.partIdx = partIdx;
    }

    public int[] getRows() {
        return rows;
    }

    public void setRows(int[] rows) {
        this.rows = rows;
    }

    public int[] getCols() {
        return cols;
    }

    public void setCols(int[] cols) {
        this.cols = cols;
    }

    public int getWorkCapacity() {
        return workCapacity;
    }

    public void setWorkCapacity(int workCapacity) {
        this.workCapacity = workCapacity;
    }

}
