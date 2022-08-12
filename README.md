# Introduction
Use `CPLEX` to solve *cutting stock problem* by **Column Generation**

---
# Model
## RMP
### Formula
$$min \sum_{j=1}^n x_j$$

$$s.t. \sum_{j=1}^n a_{ij}x_j \ge demand_i \forall i$$
### Explain
- a pattern will use a whole stock.
- $x_j$ is the time that $pattern_j$ used
- $a_ij$ is how much item used in $pattern_j$ of item $i$
- $demand_i$ is the demand of item $i$






## Sub Problem