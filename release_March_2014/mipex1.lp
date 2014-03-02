\Problem name: ilog.cplex

Minimize
 obj: IloX0 + IloX1 + IloX2 + IloX3
Subject To
 IloC0: IloX0  = 1
 IloC1: IloX1 + IloX3  = 1
 IloC2: IloX2 + IloX3  = 1
Bounds
 0 <= IloX0 <= 5
 0 <= IloX1 <= 5
 0 <= IloX2 <= 5
 0 <= IloX3 <= 5
Generals
 IloX0  IloX1  IloX2  IloX3 
End
