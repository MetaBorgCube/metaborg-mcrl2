# Inference

## Bounds

What bounds to infer?

For empty lists, we can use
- [1] Most general type allowed by use
- [2] Most specific type

For polymorphic functions, we can use
- [1] Most specific type allowed by use
- [2] Most general type

What would be reasons to pick one or the other strategy?
- Resulting number of type conversions
- Overloading

Re type conversions:
- Using [2] doesn't consider the actual types in the program. I expect it often
  to be cause conversion.
  
Re overloading:
- Should we try to 'win' overloading?
- Using [1], we might get this.

## Example 4

Initial constraints

    Bool # Bool -> Bool <: A20+ # A21+ -> A22+
    S9+ # S9+ -> Bool <: List(Pos) # List(L2-) -> A20+
    S10+ # S10+ -> Bool <: List(L2-) # List(Nat) -> A21+

Decompose

    A20+ <: Bool
    A21+ <: Bool
    Bool <: A22+
    
    List(POS) <: S9+
    List(L2-) <: S9+
    BOOL <: A20+
    
    List(L2-) <: S10+
    List(Nat) <: S10+
    Bool <: A21+

Merge bounds

    A20+ <: Bool
    A21+ <: Bool
    Bool <: A22+
    
    List(X1+) <: S9+
    Pos <: X1+
    L2- <: X1+
    Bool <: A20+
    
    List(X2+) <: S10+
    L2- <: X2+
    Nat <: X2+
    Bool <: A21+

Reorganize    
    
    Bool <: A20+ <: Bool
    Bool <: A21+ <: Bool
    Bool <: A22+

    List(X1+) <: S9+
    List(X2+) <: S10+

    Pos <: X1+
    L2- <: X1+

    L2- <: X2+
    Nat <: X2+

Interesting bit

       X1+    X2+
       / \   / \
    Pos   L2-   Nat

Add implied bounds (1)
    
       Real    Real
         |      |
        X1+    X2+
       /  \   /  \
    Pos    L2-    Nat

Propagate bounds

       Real        Real
         |          |
        X1+  Real  X2+
       /   \  |  /    \
    Pos      L2-       Nat

Add implied bounds
    
       Real        Real
         |          |
        X1+  Real  X2+
       /   \  |  /    \
    Pos      L2-       Nat
              |
             Pos

Compute types

    X1+ := lub(Pos,Pos) = Pos
    X2+ := lub(Pos,Nat) = Nat
    L2- := glb(Real,Real,Real) = Real

Alternative: Find most specific type for list.

## Example 6

Initial constraints

    Nat -> Bool <: A23+ -> A24+
    A22+ <: Pos -> A23+
    List(S9+) # Nat -> S9+ <: List(L4-) # Pos -> A22+

Decompose

    A23+ <: Nat
    Bool <: A24+

    A22+ <: Pos -> A23+
    
    List(L4-) <: List(S9+) => L4- <: S9+
    Pos <: Nat             => 
    S9+ <: A22+

Reorganize
    
    A23+ <: Nat
    Bool <: A24+

    L4- <: S9+ <: A22+ <: Pos -> A23+

Interesting bit

    Pos -> A23+
        |
       A22+
        |
       S9+
        |
       L4-

Add implied bounds

    Pos -> A23+
        |
       A22+
        |
       S9+
        |
       L4-
        |
    X1- -> X2+

Closure

    X1- -> X2+ <: Pos -> A23+

Decompose

    Pos <: X1-
    X2+ <: A23+

Next interesting bit

    Nat
     |
    A23+        X1-
     |           |
    X2+         Pos
    
Add implied bounds

    Nat        Real
     |           |
    A23+        X1-
     |           |
    X2+         Pos
     |
    Pos

Computed types

    A23+ := Pos
    X2+  := Pos
    X1-  := Real
    L4-  := Pos -> A23+ = Pos -> Pos
    A22+ := X1- -> X2+  = Real -> Pos
    S9+  := X1- -> X2+  = Real -> Pos
