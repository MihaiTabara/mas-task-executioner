=> The Facilitator assigns tasks to the agents as they show up in the list when
iterating through them. That is, should we have a list of tasks L: T1, T2, T3,
T4, and three agents: A1, A2, A3 the Facilitator splits the task as follows:
    => A1: T1, T4
    => A2: T2
    => A3: T3

* The algorithm used by the agent in order to achieve social welfare:
=> An agents computes the task corpus as follows
    -> initially, removes all the tasks it cannot execute (does not have the
    required capability); it pushes them directly to the leftovers queue
    -> sorts out the remaining list of tasks, in a *descending* order after
    cost. The idea behind this was to always try to get rid of the most
    expensive tasks as for the cheaper ones, there will always be the
    CFP alternative. After sorting them out, it iterates them over and
    either adds them to the willDo list (committed to execute - should
    it have enough budget) or to the leftovers list.

* The protocol and the communication primitives are better described in the
<arch_image.JPG> image attached within this project
