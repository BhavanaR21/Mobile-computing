package com.amsy.mobileoffloading.helper;

import com.amsy.mobileoffloading.entities.C_Workload;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WorkloadConvert {
    public static Payload toPayload(C_Workload tPayload) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(tPayload);
        objectOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();

        Payload payload = Payload.fromBytes(bytes);
        return payload;
    }

    public static C_Workload fromPayload(Payload payload) throws IOException, ClassNotFoundException {
        byte[] receivedBytes = payload.asBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivedBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (C_Workload) objectInputStream.readObject();
    }
}
