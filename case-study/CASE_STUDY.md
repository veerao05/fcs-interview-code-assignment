# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

### Key Challenges in Cost Allocation and Tracking

**1. Multi-Dimensional Cost Attribution**
The primary challenge is that costs must be tracked across multiple intersecting dimensions simultaneously (Warehouse, Store, Order, SKU, Time Period). For example, labor costs incurred in a warehouse may support orders for multiple stores, each containing different SKUs with varying handling complexity. Determining fair allocation requires a clear methodology that balances accuracy with computational feasibility.

**2. Shared Resource Allocation**
In a fulfillment environment, many resources are shared:
- Warehouse staff may pick items for multiple stores/orders in a single batch
- Transportation vehicles may consolidate shipments for multiple destinations
- Equipment (forklifts, conveyors) serves various operations simultaneously
- Overhead costs (rent, utilities, management) benefit all operations

**Challenge**: How do we fairly allocate these shared costs without creating excessive tracking overhead?

**3. Timing and Period Mismatches**
Costs are incurred at different times than when value is realized:
- Labor for picking may occur in one period, but shipping in another
- Inventory holding costs accumulate over time before the sale
- Transportation costs may be billed after delivery

**Challenge**: Which period should bear the cost? Do we use accrual-based or cash-based accounting?

**4. Variable Cost Patterns**
Some costs scale linearly with volume (e.g., picking labor), others have step functions (e.g., adding a shift), and some are fixed regardless of volume (e.g., warehouse rent). This makes per-unit cost calculations volatile, especially during low-volume periods.

**5. Data Integration and Quality**
Cost data originates from disparate sources:
- Labor costs from HR/Payroll systems
- Inventory costs from WMS and procurement systems
- Transportation from TMS or carrier invoices
- Overhead from general ledger/ERP systems

**Challenge**: Ensuring timely, accurate data flow between these systems while maintaining data consistency and auditability.

### Important Considerations

**Allocation Methodology Selection:**
- **Activity-Based Costing (ABC)**: More accurate but complex—allocates costs based on actual activities that drive costs (e.g., number of picks, cube moved, distance traveled)
- **Direct Attribution**: Where possible, directly assign costs to specific warehouses/stores (e.g., dedicated transportation lanes)
- **Proportional Allocation**: Simpler approach using volume-based metrics (e.g., units shipped, orders processed, revenue contribution)

The choice depends on the accuracy requirements, system capabilities, and cost-benefit tradeoff of implementation complexity.

**Granularity vs. Overhead:**
Finer granularity (per-SKU, per-order costing) provides better insights but increases system complexity and data storage requirements. Must determine the right level based on business value—tracking costs at the Warehouse-Store-Time Period level may be sufficient for most decisions, with drill-down capability for analysis.

**Cost Pooling Strategy:**
Define logical cost pools (e.g., "Inbound Receiving," "Storage," "Picking & Packing," "Outbound Shipping") and establish clear allocation bases for each. This provides better visibility into cost drivers and enables targeted optimization.

**Real-Time vs. Batch Processing:**
- Real-time allocation enables dynamic decision-making but requires robust systems
- Batch processing (daily/weekly) is simpler and more practical for most reporting needs
- Hybrid approach: real-time for operational decisions, batch for financial reporting

**Auditability and Transparency:**
Every cost allocation should be traceable and explainable. The system must maintain clear audit trails showing:
- Original source of cost data
- Allocation methodology applied
- Calculation logic and parameters used
- Who approved/modified allocation rules

### Critical Questions to Address

**Business Requirements:**
1. What is the primary business objective—P&L accuracy, pricing decisions, performance measurement, or budget management?
2. What level of cost accuracy is acceptable? (±1%, ±5%, ±10% tolerance)
3. How frequently do stakeholders need cost data? (Real-time, daily, weekly, monthly)
4. Which cost categories are most material and require detailed tracking vs. simplified allocation?

**Allocation Methodology:**
5. How should we allocate shared warehouse overhead across stores that use the facility?
   - By volume (units shipped)?
   - By space consumed (cubic feet)?
   - By order count?
   - By revenue contribution?
6. For transportation costs, do we allocate based on weight, distance, actual carrier charges, or a standard rate?
7. How do we handle idle capacity costs during low-volume periods?
8. Should we use standard costs (budgeted rates) or actual costs for allocation?

**Operational Considerations:**
9. How do we handle cost adjustments, returns, and cancellations?
10. What happens when a store's order is partially fulfilled from multiple warehouses?
11. How do we allocate costs for value-added services (gift wrapping, special packaging)?
12. Should we differentiate between regular and expedited fulfillment costs?

**System and Data:**
13. What is the current data latency from source systems? Can we improve it?
14. Are there regulatory or compliance requirements for cost tracking (e.g., GAAP, tax jurisdictions)?
15. How will historical cost data be preserved during warehouse transitions or reorganizations?
16. What level of automation is feasible given current system capabilities?

**Performance and Accountability:**
17. Who is accountable for cost variances—warehouse managers, store managers, or both?
18. How will cost data be used in performance evaluation and incentive structures?
19. What benchmarks or KPIs should we track? (Cost per order, cost per unit, cost per cubic foot, labor productivity)

### Related Experience and Insights

From a system design perspective, I would approach this problem by:

**1. Starting with Direct Attribution**: Maximize directly attributable costs (specific labor, dedicated lanes) before resorting to allocation formulas. This reduces disputes and improves accuracy.

**2. Establishing Clear Allocation Hierarchies**: Define primary, secondary, and tertiary allocation bases. For example:
   - Primary: Direct costs to specific warehouses/stores where identifiable
   - Secondary: Allocate warehouse-level costs to stores based on volume/activity
   - Tertiary: Allocate corporate overhead based on revenue or headcount

**3. Implementing Standard Costs with Variance Analysis**: Using standard costs for planning and budgeting, then analyzing variances against actuals, provides more stable metrics and highlights operational issues without constant rate fluctuations.

**4. Building for Flexibility**: Allocation methodologies will evolve as the business grows. The system should support configurable allocation rules without requiring code changes—allowing business users to adjust parameters as needed.

**5. Emphasizing Reconciliation Controls**: Regular reconciliation between detailed allocations and general ledger totals is critical. Any discrepancies indicate data quality issues or logic errors that must be resolved promptly.

**Key Tradeoff**: Perfect cost accuracy is often impossible and prohibitively expensive. The goal should be "fit-for-purpose" accuracy—good enough to make sound business decisions while remaining operationally feasible. A simple, transparent methodology that everyone understands and trusts is often more valuable than a complex, theoretically perfect system that becomes a black box.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

### Approach to Identifying Cost Optimization Opportunities

**1. Data-Driven Analysis**
Start with comprehensive cost visibility from Scenario 1's tracking system to identify where costs are concentrated:
- **Pareto Analysis**: Identify the 20% of cost drivers that represent 80% of total costs
- **Trend Analysis**: Review cost trends over time to spot anomalies or areas of cost creep
- **Variance Analysis**: Compare actual costs against budgets and industry benchmarks
- **Cost-per-Unit Metrics**: Calculate cost per order, cost per unit shipped, cost per cubic foot to identify inefficiencies

**2. Process Mining and Observation**
Quantitative data alone doesn't reveal all opportunities:
- Observe warehouse operations to identify bottlenecks, waste, and inefficient workflows
- Map end-to-end fulfillment processes to spot redundancies
- Gather frontline feedback from warehouse staff who experience inefficiencies daily
- Review exception handling—errors, returns, and rework often hide significant costs

**3. Benchmarking**
Compare performance against internal and external standards:
- Cross-warehouse comparisons to identify best practices
- Industry benchmarks for labor productivity, space utilization, transportation costs
- Competitor analysis (where possible) for service level vs. cost tradeoffs

**4. Technology Assessment**
Evaluate if current systems and automation levels are optimal:
- Are manual processes consuming excessive labor that could be automated?
- Is existing technology underutilized or creating inefficiencies?
- What emerging technologies could deliver step-change improvements?

### Prioritization Framework

Not all cost savings are equal. Prioritize based on a multi-factor scorecard:

**Impact vs. Effort Matrix:**
- **High Impact, Low Effort** (Quick Wins): Implement immediately
- **High Impact, High Effort** (Strategic Projects): Plan carefully, allocate resources
- **Low Impact, Low Effort** (Fill-ins): Do when capacity allows
- **Low Impact, High Effort** (Avoid): Defer or skip

**Evaluation Criteria:**
1. **Financial Impact**: Estimated annual cost savings (absolute dollars and percentage)
2. **Implementation Complexity**: Time, resources, technical difficulty, change management needs
3. **Risk to Service Quality**: Likelihood and magnitude of negative customer impact
4. **Payback Period**: How quickly will savings offset implementation costs?
5. **Strategic Alignment**: Does this support broader business objectives (e.g., sustainability, scalability)?
6. **Dependencies**: Are other initiatives required first?

**Sequencing Considerations:**
- **Build momentum**: Start with quick wins to demonstrate ROI and build organizational support
- **Foundation first**: Some initiatives enable others (e.g., data infrastructure before advanced analytics)
- **Risk mitigation**: Pilot high-risk changes in limited scope before full rollout
- **Resource constraints**: Don't overload the organization with too many simultaneous changes

### Implementation Approach

**Phase 1: Pilot and Validate**
- Start with a controlled pilot (single warehouse, single store, limited SKU set)
- Define clear success metrics and measurement methodology
- Run A/B tests where possible to isolate the impact of the change
- Gather feedback from all stakeholders (operations, finance, stores)
- Refine the approach based on learnings

**Phase 2: Scale and Optimize**
- Develop rollout plan with clear milestones
- Create playbooks and training materials
- Establish ongoing monitoring and adjustment mechanisms
- Build in flexibility to adapt to local conditions

**Phase 3: Institutionalize and Sustain**
- Embed new processes into standard operating procedures
- Update training programs for new hires
- Create incentive structures that reinforce desired behaviors
- Establish continuous improvement cycles to prevent regression

**Critical Success Factors:**
- **Executive Sponsorship**: Visible leadership support for change initiatives
- **Change Management**: Proactive communication, training, and addressing resistance
- **Cross-Functional Collaboration**: Finance, operations, IT, and business units must work together
- **Measurement Discipline**: Track actual results vs. projections; course-correct quickly

### Specific Cost Optimization Strategies and Expected Outcomes

**Labor Optimization**

*Strategy 1: Warehouse Layout and Slotting Optimization*
- **Action**: Position high-velocity SKUs closer to packing stations; optimize pick paths
- **Expected Outcome**: 10-20% reduction in picking time; 15-25% increase in picks per hour
- **Risk**: Requires analysis effort and potential disruption during reslotting

*Strategy 2: Labor Scheduling Alignment*
- **Action**: Use predictive analytics to match staffing levels to demand patterns (hourly, daily, seasonal)
- **Expected Outcome**: 5-15% reduction in labor costs through better capacity utilization; reduced overtime
- **Risk**: Scheduling complexity; potential service issues if forecast is inaccurate

*Strategy 3: Cross-Training and Flexible Workforce*
- **Action**: Train staff across multiple functions (receiving, picking, packing, loading)
- **Expected Outcome**: Improved labor utilization during demand fluctuations; 10-15% productivity gain
- **Risk**: Training costs; some skill dilution vs. specialization

**Inventory and Storage Optimization**

*Strategy 4: Inventory Positioning and Distribution Network Design*
- **Action**: Analyze which SKUs should be stocked at which warehouses based on demand patterns
- **Expected Outcome**: 15-30% reduction in transportation costs; 10-20% reduction in inventory holding costs; faster delivery
- **Risk**: Complex analysis; may require system changes; risk of stockouts during transition

*Strategy 5: Inventory Level Optimization*
- **Action**: Implement demand-driven safety stock models; reduce slow-moving inventory
- **Expected Outcome**: 10-25% reduction in inventory carrying costs; improved cash flow
- **Risk**: Increased stockout risk if models are wrong; requires supplier reliability

*Strategy 6: Space Utilization Improvements*
- **Action**: Implement vertical storage solutions; optimize bin sizes; use dynamic slotting
- **Expected Outcome**: 20-40% increase in storage density; defer/avoid warehouse expansion costs
- **Risk**: Upfront capital investment; may slow pick rates if not designed well

**Transportation and Logistics Optimization**

*Strategy 7: Route Optimization and Load Consolidation*
- **Action**: Use advanced routing algorithms; consolidate shipments to common destinations
- **Expected Outcome**: 10-20% reduction in transportation costs; improved vehicle utilization from 70% to 85%+
- **Risk**: May increase transit time if consolidation delays shipments

*Strategy 8: Carrier Mix Optimization*
- **Action**: Analyze carrier performance and rates; negotiate better contracts; use multi-carrier strategy
- **Expected Outcome**: 5-15% reduction in freight costs through competitive pressure and optimization
- **Risk**: Complexity in managing multiple carrier relationships

*Strategy 9: Packaging Optimization*
- **Action**: Right-size boxes; reduce dunnage; use standardized packaging
- **Expected Outcome**: 5-10% reduction in material costs; 3-8% reduction in shipping costs (dimensional weight)
- **Risk**: Product damage if packaging is insufficient

**Technology and Automation**

*Strategy 10: Warehouse Management System (WMS) Optimization*
- **Action**: Fully utilize existing WMS features (wave planning, task interleaving, directed put-away)
- **Expected Outcome**: 10-20% productivity improvement with zero capital investment
- **Risk**: Requires training and process changes; may reveal system limitations

*Strategy 11: Automation Where Justified*
- **Action**: Implement targeted automation (conveyor systems, automated storage/retrieval, sortation)
- **Expected Outcome**: 30-50% labor reduction in automated areas; improved accuracy; 24/7 operation potential
- **Risk**: High capital cost; long payback periods (2-5 years); inflexibility to changing needs

*Strategy 12: Paperless Operations*
- **Action**: Deploy mobile devices, barcode scanning, digital pick lists
- **Expected Outcome**: 5-10% productivity improvement; near-zero error rates; real-time visibility
- **Risk**: Device costs; training; system reliability becomes critical

**Process and Quality Improvements**

*Strategy 13: Error Reduction and Quality Control*
- **Action**: Implement pick verification, automated quality checks, root cause analysis
- **Expected Outcome**: 50-80% reduction in errors; 20-30% reduction in return processing costs
- **Risk**: May slow operations if checks are too burdensome

*Strategy 14: Returns Processing Optimization*
- **Action**: Streamline returns inspection, disposition, and restocking processes
- **Expected Outcome**: 30-50% reduction in returns processing costs; faster inventory return to saleable state
- **Risk**: Requires careful balance between speed and proper inspection

*Strategy 15: Supplier Collaboration*
- **Action**: Work with suppliers on better packaging, advance ship notices (ASNs), drop-shipping
- **Expected Outcome**: 10-20% reduction in inbound receiving labor; reduced inventory needs
- **Risk**: Requires supplier capability and willingness to collaborate

**Overhead and Indirect Cost Management**

*Strategy 16: Energy Efficiency*
- **Action**: LED lighting, HVAC optimization, solar panels, efficient equipment
- **Expected Outcome**: 10-30% reduction in utility costs
- **Risk**: Upfront investment; payback periods vary

*Strategy 17: Procurement and Vendor Management*
- **Action**: Consolidate vendors; negotiate volume discounts; standardize supplies
- **Expected Outcome**: 5-15% reduction in MRO and supplies costs
- **Risk**: Vendor concentration risk

### Critical Questions to Address

**Understanding Current State:**
1. What is the current cost breakdown by category (labor, inventory, transportation, overhead)?
2. Which cost categories have increased most over the past 1-2 years? Why?
3. What are the current service level metrics (on-time delivery, accuracy, damage rates)?
4. What are the non-negotiable service requirements that cannot be compromised?

**Identifying Opportunities:**
5. Where do actual costs significantly exceed budgets or benchmarks?
6. Which warehouses are most/least efficient? What drives the difference?
7. What manual processes are most labor-intensive and error-prone?
8. What are the top 3 pain points from operations managers?

**Prioritization and Tradeoffs:**
9. What is the target cost reduction (dollar amount or percentage)?
10. What is the acceptable payback period for capital investments?
11. How do we balance short-term wins vs. long-term strategic improvements?
12. What level of risk to service quality is acceptable during optimization efforts?
13. Are there specific constraints (e.g., no headcount reductions, no capital budget)?

**Implementation Planning:**
14. What is the organization's change capacity? How many initiatives can we handle simultaneously?
15. Who will own each optimization initiative, and what are their accountability metrics?
16. What baseline measurements exist today to prove impact?
17. How will we handle potential pushback from store teams if fulfillment changes affect them?
18. What contingency plans exist if an optimization creates service issues?

**Technology and Capabilities:**
19. What is the condition and capability of current WMS/TMS systems?
20. What data infrastructure exists to support advanced analytics (demand forecasting, route optimization)?
21. Are there automation pilots or proofs-of-concept we can learn from?

**Measurement and Governance:**
22. How will we track and report progress on cost optimization initiatives?
23. What governance structure will oversee the optimization portfolio?
24. How frequently will we review and adjust priorities?

### Experience-Based Insights

**1. Start with Process Before Technology**
Many organizations jump to automation or new systems when process improvements could deliver 50%+ of the benefit at 10% of the cost. Optimize workflows first, then automate the optimized process.

**2. Don't Underestimate Change Management**
Technical solutions often fail due to people factors. Involve frontline staff early, explain the "why," address fears about job security, and celebrate early wins to build momentum.

**3. Beware of Cost Shifting**
Some "optimizations" simply shift costs elsewhere. For example, reducing warehouse costs by shipping from fewer locations may increase transportation costs. Always look at total fulfillment cost, not just one silo.

**4. Maintain Service Quality**
Cost cutting that erodes service quality is self-defeating—it drives customer complaints, returns, and ultimately lost revenue. Define clear service level guardrails and monitor them religiously during optimization efforts.

**5. Use Pilot Programs Ruthlessly**
Never roll out major changes without rigorous pilots. Pilots reveal unintended consequences, allow refinement, and build proof points that reduce resistance to change.

**6. Create a Portfolio Approach**
Balance quick wins (high ROI, low risk) with strategic bets (transformational but longer-term). Quick wins fund and build credibility for bigger initiatives.

**7. Institutionalize Continuous Improvement**
Cost optimization isn't a one-time project—it's a mindset. Establish regular kaizen events, idea programs, and performance reviews that keep improvement front-and-center.

**8. Watch for Diminishing Returns**
The first 10-15% of cost reduction is usually achievable with reasonable effort. Getting to 20-25% requires increasingly complex initiatives with higher risk. Be realistic about what's achievable without compromising core capabilities.

**Example Sequencing:**
- **Month 1-3**: Quick wins (layout optimization, WMS feature activation, vendor negotiations) targeting 5-8% cost reduction
- **Month 4-9**: Medium-term projects (labor scheduling systems, inventory optimization, carrier mix) targeting additional 7-10%
- **Month 10-18**: Strategic initiatives (automation pilots, network redesign) targeting additional 5-10%
- **Ongoing**: Continuous improvement culture, sustaining gains, iterating on solutions

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Integration with financial systems is critical - without it, you end up with duplicate data entry, reconciliation headaches, and the inevitable "which number is right?" discussions. I've seen teams spend days reconciling operational data with financial records because systems weren't properly integrated.

The main benefits I see:

First, single source of truth. When cost data flows directly from the Cost Control Tool into the financial system (or vice versa), you eliminate manual entry errors and ensure everyone's looking at the same numbers. Finance doesn't have to question operations data, and operations can trust that their cost tracking aligns with what's in the books.

Second, real-time visibility. If integration is done right, finance can see fulfillment costs as they happen instead of waiting for month-end reports. That enables faster decision-making - like catching a cost spike early instead of discovering it weeks later when it's too late to do anything about it.

Third, reduced manual work. Nobody enjoys exporting CSVs, massaging data in Excel, and importing into another system. Good integration eliminates that grunt work and frees people up for actual analysis.

Fourth, better audit trail. When data flows automatically between systems, you have a clear record of what cost was recorded when, and where it came from. That makes audits much less painful.

Now, "real-time data synchronization" sounds great in theory, but I'd want to understand what that really means here. True real-time (sub-second latency) is expensive and complex. Do we actually need that, or is near-real-time (within minutes) good enough? For most use cases, I suspect batch sync every 15-30 minutes would work fine and be much simpler to implement.

For ensuring seamless integration, I'd think about:

**Understanding the existing landscape** - What financial systems are we integrating with? Is it SAP, Oracle, NetSuite, or something custom? What APIs or integration points do they expose? Are we dealing with modern REST APIs or legacy batch file transfers? The integration approach depends heavily on what we're working with.

**Data mapping and transformation** - Cost categories in the operational system probably don't map 1:1 to GL accounts. We need clear mappings between operational cost types and financial accounts. Who owns maintaining those mappings when chart of accounts changes?

**Error handling and reconciliation** - Integrations fail. Network issues, system downtime, data validation errors - it all happens. How do we handle failures? Do we retry automatically? Alert someone? Most importantly, how do we ensure we don't lose transactions or double-count costs?

**Data consistency and timing** - If the Cost Control Tool calculates a cost allocation and pushes it to finance, but then the allocation gets adjusted (maybe a correction or a return), how do we handle that? Do we update the original transaction or post a reversal and a new entry? This matters a lot for financial reporting.

**Security and access control** - Financial data is sensitive. The integration needs proper authentication, encrypted data transfer, and audit logging. We also need to think about who can initiate data syncs and who can see what.

**Performance and scalability** - How much data are we moving? If we're syncing thousands of cost transactions per hour, we need to make sure neither system gets overwhelmed. Batch processing might be better than individual transaction syncs.

Questions I'd want answered:

- What financial system(s) are we integrating with? What's their API/integration capability?
- Is this a one-way sync (Cost Control → Finance) or bidirectional?
- What's the actual latency requirement? Do we really need real-time or is hourly/daily acceptable?
- Who owns the master data? For example, if warehouse codes exist in both systems, which one is the source of truth?
- How do we handle corrections and adjustments after data has been synced?
- What's the approval workflow? Do costs get synced automatically or does someone review them first?
- Are there any data transformation requirements? Like currency conversions or cost allocation rules that need to be applied?
- What happens during month-end close? Do we pause syncing to ensure financial reports are stable?
- How do we handle historical data migration if this is a new integration?

From a technical standpoint, I'd probably look at:

Using an integration layer or middleware if we're dealing with multiple systems - something that can handle message queuing, retries, and data transformation. Could be a dedicated integration platform or even just a well-designed microservice.

Event-driven architecture where possible - when a cost is recorded, emit an event that the financial system can consume. This decouples the systems and makes it easier to add more consumers later.

Idempotency - make sure that if a sync runs twice (due to a retry or error), it doesn't create duplicate entries. Using unique transaction IDs helps here.

Comprehensive logging and monitoring - we need visibility into what data is flowing between systems, when it's flowing, and any failures. Without this, troubleshooting becomes nearly impossible.

A reconciliation process even with integration - trust but verify. Regular automated checks to ensure totals in the Cost Control Tool match what's in the financial system. Any discrepancies should trigger alerts.

One thing I'd be cautious about - don't try to build the perfect integration on day one. Start with the most critical data flows, get those working reliably, then expand. It's better to have a simple, reliable integration for core cost data than a complex, fragile integration trying to sync everything.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
[ fill here your answer ]

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
[ fill here your answer ]

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
