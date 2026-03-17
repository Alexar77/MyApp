# Goals V1: Structured Dark Mobile

Design a premium mobile Goals screen for MyApp, a habit and personal organization app. The screen should feel like a clean SaaS mobile product in dark mode, inspired by Linear and modern task apps. Highly readable, tidy, and action-oriented.

This screen manages:
- goals to achieve
- goals achieved
- expandable goal cards
- subgoals within each goal
- add goal action
- quick actions to rename, delete, expand, and add subgoal
- toggles for marking goals and subgoals as done

Required UI structure:
- top app bar with title “Goals”
- floating add goal button
- two collapsible sections:
  - Goals to achieve
  - Goals achieved
- inside each section show goal cards
- each goal card should include:
  - done / not done icon
  - goal title
  - optional completed date if done
  - actions: add subgoal, rename, delete, expand/collapse
- expanded goal card should show subgoal rows with:
  - done toggle
  - subgoal title
  - optional completed date
  - rename and delete actions

Important product rule to reflect:
- a goal cannot be marked done until all subgoals are complete
- show this visually through locked, disabled, or helper-state design

Style:
- dark graphite background
- elevated cards with subtle borders
- accent color: blue or teal
- success green for completed states
- muted secondary text for dates and helper labels
- minimal but premium spacing and hierarchy

Typography:
- use a sharp modern sans serif such as SF Pro, Inter Tight, or Geist
- goal titles should feel strong
- section headers should feel structured, not decorative

Use sample content:
- 3 active goals
- 2 achieved goals
- one expanded active goal with 3 subgoals, not all complete
- one achieved goal with a visible completion date

Output one mobile screen that feels practical, polished, and launch-ready.
