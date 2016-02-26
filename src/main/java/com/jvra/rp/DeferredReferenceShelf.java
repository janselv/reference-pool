package com.jvra.rp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jansel Valentin on 2/2/2015.
 */


public class DeferredReferenceShelf<T> implements Closeable {

    private Log log;

    private int shelvesBoundary;
    private AtomicInteger liveResources = new AtomicInteger(0);
    private ResourceHandler<T> handler;

    private ConcurrentLinkedQueue<T> availableResources = new ConcurrentLinkedQueue<T>();
    private Map<Object, T> inUseResources = new ConcurrentHashMap<Object, T>();

    private ReferenceQueue<DeferredReference<? extends T>> refQueue = new ReferenceQueue<DeferredReference<? extends T>>();

    private ShelvesBoundarySqueezer<T> squeezer;

    public DeferredReferenceShelf(int shelvesBoundary, String name, ResourceHandler<T> handler) {
        this.shelvesBoundary = shelvesBoundary;
        this.handler = handler;
        log = LogFactory.getLog("[DeferredReferenceShelf:" + name + "]");
        squeezer = new ShelvesBoundarySqueezer<T>(this);
    }

    public DeferredReference<T> take() throws InterruptedException {
        if (!squeezer.isRunning())
            squeezer.start();

        for (; ; ) {
            if (0 < availableResources.size()) {
                T b = availableResources.remove();
                log.info("Returning cached buffer: " + System.identityHashCode(b));
                return wrapResource(b);
            }
            T resource = handler.create();
            log.info("Creating resource " + System.identityHashCode(resource));
            liveResources.incrementAndGet();
            return wrapResource(resource);
        }
    }


    private DeferredReference<T> wrapResource(T resource) {
        DeferredReference<T> wrapper = new DeferredReference<T>(resource);
        PhantomReference<DeferredReference<? extends T>> ref = new PhantomReference<DeferredReference<? extends T>>(wrapper, refQueue);
//        log.info("Associating resource " + System.identityHashCode(ref) + " with reference " + ref);
        inUseResources.put(ref, resource);
        return wrapper;
    }

    public Log getLog() {
        return log;
    }

    @Override
    public void close() throws IOException {
        squeezer.stop();
    }


    private static class ShelvesBoundarySqueezer<T> implements Runnable {
        private DeferredReferenceShelf<T> shelf;
        private volatile boolean running;
        private Log log;

        private ShelvesBoundarySqueezer(DeferredReferenceShelf<T> shelf) {
            this.shelf = shelf;
            prepareLogger(shelf);
        }

        private void prepareLogger(DeferredReferenceShelf<T> shelf) {
            if (null != shelf && null != shelf.getLog())
                log = shelf.getLog();
            else
                log = LogFactory.getLog(DeferredReferenceShelf.class);
        }

        @Override
        public void run() {
            if (null == shelf)
                return;
            running = true;
            int i = 0;
            while (running) {
                try {
                    Reference<?> ref = shelf.refQueue.remove(1000);
                    if (null != ref) {
                        if (shelf.availableResources.size() < shelf.shelvesBoundary) {
                            final T resource = shelf.inUseResources.get(ref);
                            if (null != resource) {
                                log.info("Making available resource: " + System.identityHashCode(resource) + " of reference " + ref);
                                shelf.handler.clean(resource);
                                shelf.availableResources.offer(resource);
                                shelf.inUseResources.remove(ref);
                            }
                        } else {
                            Object removed = shelf.inUseResources.remove(ref);
                            shelf.liveResources.decrementAndGet();
                            log.info("Removing unnecessary resource " + System.identityHashCode(removed) + " of reference " + ref);
                        }
                    }
                    if (i == 15) {
                        log.info("ALL RESOURCES availableResources=>" + shelf.availableResources.size() + " inUseResources=> " + shelf.inUseResources.size() + " : liveResources=>" + shelf.liveResources.get());
                        i = 0;
                    }
                    ++i;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            log.info("Exiting from squeezer...");
        }

        public boolean isRunning() {
            return running;
        }

        public void start() {
            new Thread(this).start();
        }

        public void stop() {
            running = false;
        }
    }
}