package com.jvra.rp.example;

import com.jvra.rp.DeferredReferenceShelf;
import com.jvra.rp.ResourceHandler;

import javax.servlet.ServletContextEvent;
import java.nio.ByteBuffer;

/**
 * Created by Jansel Valentin on 2/2/2015.
 */
public class BBServletContextListener implements javax.servlet.ServletContextListener{

    private static final int MAX_BUFFERS = 30;

    private static DeferredReferenceShelf<ByteBuffer> shelf;

    public static final DeferredReferenceShelf<ByteBuffer> get(){
        checkShelf("");
        return shelf;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String name = "";
        try{
            name = sce.getServletContext().getContextPath();
        }catch ( Exception e){}

        checkShelf(name);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if( null != shelf )
                shelf.close();
        }catch ( Exception ex ){
            ex.printStackTrace();
        }
    }

    private static void checkShelf( String name ) {
        synchronized (BBServletContextListener.class) {
            if (null != shelf)
                return;
        }
        synchronized (BBServletContextListener.class) {
            if (null == shelf) {
                shelf = new DeferredReferenceShelf<>(MAX_BUFFERS, name,
                        new ResourceHandler<ByteBuffer>() {
                            @Override
                            public ByteBuffer create() {
                                return ByteBuffer.allocateDirect(4096);
                            }

                            @Override
                            public void clean(ByteBuffer resource) {
                                if (null != resource)
                                    resource.clear();
                            }
                        });
            }
        }
    }
}
