# Pharmacy Workflow Helper (CIS-18A Course Project – Option 1)

A simple Java console with Swing GUI program that helps triage pharmacy orders by priority and deadlines. It generates consistent handoff messages for compounding and delivery.

This project targets one specific real-world workflow problem that is missing within my day to day workplace:
tracking what’s due today now vs later, and keeping a clear “work queue” that updates when an order changes priority.

## Features

### Phase 1 Console control
- Add an order using a non-PHI Order ID, Priority, and optional Notes  
- Add-or-Update behavior (if the Order ID already exists, the order is updated instead of duplicated) 
- View:
  - All orders (sorted)
  - Due today
  - Overdue
  - Work Queue (Due Today → On Track → Overdue)
- Generate standardized scripts:
  - Handoff script
  - Compounding script
  - Delivery script
- Save a daily report to a text file
- Simulate time using Current Day (Day 0, Day 1, Day 2…)

### Phase 2 (GUI - Swing)
- All core features available via a simple GUI:
  - Add/Update
  - Explicit update
  - Work Queue view
  - Due Today / Overdue / All
  - Generate scripts
  - Advance day
  - Save report

---

## Priority / Deadline Rules
- **STAT:** Due **today** and should be completed first
- **URGENT:** Due **today** (end-of-day) for overnight delivery
- **ROUTINE:** Due in **2 days** (Day + 2)

 Note: This model uses a simple day counter instead of real clock time 

## OOP Concepts Used
- **Encapsulation:** `Order` controls updates through methods (`update()`), keeping internal state consistent.
- **Abstraction:** Orders share a common structure and behaviors (status, display formatting).
- **Polymorphism / Overloading (where applicable):** Methods provide multiple “views” and behaviors across classes.

## S.O.L.I.D Best Practices applied
- **Single Responsibility:** each class has one main job (Order, Manager, ScriptGenerator, UI).
- **Open/Closed:** new priority rules or script formats can be expanded with minimal changes.
- **DRY:** script formatting and sorting logic are centralized (not repeated in multiple places).


Compile and Run using Java JDK 21+ installed or newer

### Compile from the project folder using command prompt

javac PharmacyWorkflowHelper.java PharmacyWorkflowGUI.java
