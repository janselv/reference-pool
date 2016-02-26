Java Resource Pool
========

Simple java resource pool that act on objects reference and reclaim unused ones possibly marked to be garbage collected.
Given a capacity for the pool size, client can request resources until that capacity is reached, when that happens
new objects are created but the pool keep digging for unused resources and try to squeeze the slots that overflow the pool
capacity trying to keep serving resources that fall into the pool capacity, in that way you are guarantee to get valid
resources.

Resource that are under pool capacity are reclaimed when unused, those that overflow pool capacity are detached
from memory when unused.


Usage
----

There are an example included as a part of the repository, but to start playing just create an instance of the pool

``` java

    DeferredReferenceShelf<ByteBuffer> pool = new DeferredReferenceShelf<>(2, 
        "Pool", new ResourceHandler<ByteBuffer>() {
        @Override
        public ByteBuffer create() {
            ByteBuffer.allocateDirect(4095);
        }

        @Override
        public void clean(ByteBuffer resource) {
            resource.clear();
        }
    });

    ...
    pool.take();
    pool.close();

```

Constribution
---------

- You may fork this library and modify it as you need.
- You can make a pull request and propse a push request.
- Report bugs.


Inspirition
--------

I took the concept of [Leaky Bucket](https://en.wikipedia.org/wiki/Leaky_bucket) algorithm for this implementation.






