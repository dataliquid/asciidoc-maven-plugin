package com.dataliquid.maven.asciidoc.pool;

import org.asciidoctor.Asciidoctor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Thread-safe pool of Asciidoctor instances for parallel processing. Uses
 * BlockingQueue for automatic thread synchronization.
 */
public class AsciidoctorPool implements AutoCloseable {

    private final BlockingQueue<Asciidoctor> pool;
    private final List<Asciidoctor> allInstances;

    /**
     * Creates a pool with the specified number of Asciidoctor instances.
     *
     * @param  size                     number of instances to create
     * @param  initializer              optional callback to configure each instance
     *                                  after creation
     *
     * @throws IllegalArgumentException if size is not positive
     */
    public AsciidoctorPool(int size, Consumer<Asciidoctor> initializer) {
        if (size <= 0) {
            throw new IllegalArgumentException("Pool size must be positive, got: " + size);
        }

        this.pool = new LinkedBlockingQueue<>(size);
        this.allInstances = new ArrayList<>(size);

        // Pre-create and initialize instances
        for (int i = 0; i < size; i++) {
            Asciidoctor instance = Asciidoctor.Factory.create();
            if (initializer != null) {
                initializer.accept(instance);
            }
            pool.offer(instance);
            allInstances.add(instance);
        }
    }

    /**
     * Creates a pool with the specified number of Asciidoctor instances without
     * custom initialization.
     *
     * @param  size                     number of instances to create
     *
     * @throws IllegalArgumentException if size is not positive
     */
    public AsciidoctorPool(int size) {
        this(size, null);
    }

    /**
     * Acquires an Asciidoctor instance from the pool. Blocks if no instance is
     * available until one is released.
     * <p>
     * <b>IMPORTANT:</b> Caller MUST call {@link #release(Asciidoctor)} in a finally
     * block to prevent resource leaks.
     * </p>
     *
     * @return                      an Asciidoctor instance
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public Asciidoctor acquire() throws InterruptedException {
        return pool.take();
    }

    /**
     * Returns an Asciidoctor instance to the pool.
     *
     * @param instance the instance to return
     */
    public void release(Asciidoctor instance) {
        if (instance != null) {
            pool.offer(instance);
        }
    }

    /**
     * Shuts down all Asciidoctor instances in the pool.
     */
    @Override
    public void close() {
        allInstances.forEach(Asciidoctor::shutdown);
        pool.clear();
    }
}
