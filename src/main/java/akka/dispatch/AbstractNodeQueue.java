/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

/*
 * This is based on akka 2.5.26
 */
package akka.dispatch;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free MPSC linked queue implementation based on Dmitriy Vyukov's non-intrusive MPSC queue:
 * http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue
 * 
 * This queue could be wait-free (i.e. without the spinning loops in peekNode and pollNode) if
 * it were permitted to return null while the queue is not quite empty anymore but the enqueued
 * element is not yet visible. This would break actor scheduling, though.
 */
@SuppressWarnings({"serial", "squid:S1948", "squid:S1948", "squid:S1994", "squid:ModifiersOrderCheck", "squid:S1181", "squid:ClassVariableVisibilityCheck", "squid:S00116", "squid:S00115"})
public abstract class AbstractNodeQueue<T> extends AtomicReference<AbstractNodeQueue.Node<T>> {

    /*
     * Extends AtomicReference for the "head" slot (which is the one that is appended to) since
     * there is nothing to be gained by going to all-out Unsafe usage—we’d have to do
     * cache-line padding ourselves.
     */
  
    private volatile Node<T> tail;

    private static long nodeCounter = 0;

    private final long nodeNumber = nodeCounter++;

    protected AbstractNodeQueue() {
       final Node<T> n = new Node<T>();
       tail = n;
       set(n);
    }

    /**
     * Query the queue tail for the next element without dequeuing it.
     * 
     * Use this method only from the consumer thread!
     * 
     * !!! There is a copy of this code in pollNode() !!!
     * 
     * @return queue node with element inside if there was one, or null if there was none
     */
    @SuppressWarnings("unchecked")
    protected final Node<T> peekNode() {
        final Node<T> tail = this.tail;
        Node<T> next = tail.next();
        if (next == null && get() != tail) {
            // if tail != head this is not going to change until producer makes progress
            // we can avoid reading the head and just spin on next until it shows up
            do {
                next = tail.next();
            } while (next == null);
        }
        return next;
    }
    
    /**
     * Query the queue tail for the next element without dequeuing it.
     * 
     * Use this method only from the consumer thread!
     * 
     * @return element if there was one, or null if there was none
     */
    public final T peek() {
        final Node<T> n = peekNode();
        return (n != null) ? n.value : null;
    }

    /**
     * Add an element to the head of the queue.
     * 
     * This method can be used from any thread.
     * 
     * @param value the element to be added; must not be null
     */
    public final void add(final T value) {
        final Node<T> n = new Node<T>(value);
        Node<T> o = getAndSet(n);
        o.setNext(n);
    }
    
    /**
     * Add an element to the head of the queue, providing the queue node to be used.
     * 
     * This method can be used from any thread.
     * 
     * @param n the node containing the element to be added; both must not be null
     */
    public final void addNode(final Node<T> n) {
        n.setNext(null);
        Node<T> o = getAndSet(n);
        o.setNext(n);
    }

    /**
     * Query the queue whether it is empty right now.
     * 
     * This method can be used from any thread.
     * 
     * @return true if queue was empty at some point in the past
     */
    public final boolean isEmpty() {
        boolean res = tail == get();
        return res;
    }

    /**
     * This method returns an upper bound on the queue size at the time it
     * starts executing. It may spuriously return smaller values (including
     * zero) if the consumer pulls items out concurrently.
     * 
     * This method can be used from any thread.
     * 
     * @return an upper bound on queue length at some time in the past
     */
    @SuppressWarnings("unchecked")
    public final int count() {
        int count = 0;
        final Node<T> head = get();
        for(Node<T> n = tail.next();
            n != null && count < Integer.MAX_VALUE; 
            n = n.next()) {
          ++count;
          // only iterate up to the point where head was when starting: this is a moving queue!
          if (n == head) break;
        }
        return count;
    }

    /**
     * Pull one item from the queue’s tail if there is one.
     * 
     * Use this method only from the consumer thread!
     * 
     * @return element if there was one, or null if there was none
     */
    public final T poll() {
        final Node<T> next = pollNode();
        if (next == null) return null;
        else {
          T value = next.value;
          next.value = null;
          return value;
        }
    }
    
    /**
     * Pull one item from the queue, returning it within a queue node.
     * 
     * Use this method only from the consumer thread!
     * 
     * @return queue node with element inside if there was one, or null if there was none
     */
    @SuppressWarnings("unchecked")
    public final Node<T> pollNode() {
      final Node<T> tail = this.tail;
      Node<T> next = tail.next();
      long spins = 0;
      if (next == null && get() != tail) {
          // if tail != head this is not going to change until producer makes progress
          // we can avoid reading the head and just spin on next until it shows up
          do {
              spins++;
              next = tail.next();
              if (spins == 1000000L) {
                  System.out.println("[pollNode] (" + tail + "); tail.next(): " + tail.next() + "; tail.next: " + tail.next);
                  System.out.flush();
                  System.exit(66);
              }
          } while (next == null);
      }
      if (next == null) return null;
      else {
        tail.value = next.value;
        next.value = null;
        this.tail = next;
        tail.setNext(null);
        return tail;
      }
    }

    public static class Node<T> {
        private static long nodeCounter = 0;

        private final long nodeNumber = nodeCounter++;

        public T value;

        private volatile Node<T> next;

        public Node() {
            this(null);
        }

        public Node(final T value) {
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        public final Node<T> next() {
            return next;
        }

        protected final void setNext(final Node<T> newNext) {
            next = newNext;
            System.out.println("[setNext] (" + this + ") " + "; newNext: " + newNext + "; next: " + next + "; next(): " + next());
        }

        @Override
        public String toString() {
            return "Node(" + nodeNumber + ")";
        }
    }
}
