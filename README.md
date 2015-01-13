# DIAS - Dynamic Intelligent Aggregation Service

This project is the source code that prototypes DIAS as illustrated in the following published paper:

Evangelos Pournaras, Martijn Warnier and Frances M.T. Brazier, A Generic and Adaptive Aggregation Service for Large-scale Decentralized Networks, Complex Adaptive Systems Modeling, 1:19, 2013 © SpringerOpen

######################################################################################################################

This is a summary of DIAS from the above published paper:

# Purpose

Purpose: Aggregation functions are used in distributed environments to make system-wide information locally available in the nodes of a network. The computation of different aggregation functions, e.g., SUMMATION, AVERAGE, MAXIMUM etc., in large-scale distributed systems is challenging and crucial for a wide range of applications. This is especially the case when the input values of these functions dynamically change during system runtime. Related approaches of decentralized aggregation are function-dependent, interaction-dependent, assume static values or cannot always tolerate duplicates and continuously changing information.

Methods: This paper introduces DIAS, the Dynamic Intelligent Aggregation Service. DIAS is an agent-based middleware that addresses these issues with a holistic approach: an efficient availability of the distributed information in every node of the network that enables the simultaneous computation of almost any aggregation function. Such an abstraction initially requires a significant communication and storage cost and has a rather large overhead. These issues are resolved by introducing an implicit local representation and storage of the explicit distributed information: aggregation memberships in bloom filters.

Results: The performance impact of bloom filters in DIAS is critical for its applicability as it compensates and reduces the initial high communication and storage required for such an abstraction.

Conclusions: Experimental evaluation under various aggregation and resource-constrained settings shows that DIAS is an efficient and accurate decentralized aggregation service.

Keywords: Aggregation; Adaptation; Agent; Bloom filter; Consistency
