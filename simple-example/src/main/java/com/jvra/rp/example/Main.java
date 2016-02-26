package com.jvra.rp.example;

import com.jvra.rp.DeferredReference;
import com.jvra.rp.DeferredReferenceShelf;
import com.jvra.rp.ResourceHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jansel Valentin on 2/2/2015.
 **/
public class Main {

    private static DeferredReferenceShelf<ByteBuffer> pool = new DeferredReferenceShelf<>(2, "Pool", new ResourceHandler<ByteBuffer>() {
        @Override
        public ByteBuffer create() {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4095);
            System.out.println("Creating buffer " + System.identityHashCode(buffer));
            return buffer;
        }

        @Override
        public void clean(ByteBuffer resource) {
            resource.clear();
        }
    });

    void execute() throws InterruptedException {
        synchronized (pool) {
            DeferredReference bufferRef = pool.take();
            System.out.println(Thread.currentThread().getName()+ " using buffer "+System.identityHashCode(bufferRef.getResource())) ;
            System.gc();
        }
    }

    public static void main(String... arg) throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new Main().execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }

        /**
         * Let us see how unused resource become available and those that overflow our shelve boundary are detached from memory
         */
        try{
            TimeUnit.MINUTES.sleep(5);
            pool.close();
        }catch( InterruptedException | IOException e){
            e.printStackTrace();
        }
    }
}
