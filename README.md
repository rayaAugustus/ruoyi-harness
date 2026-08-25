# RuoYi Harness

> Agent-native enterprise application runtime based on RuoYi ecosystem.

## Vision

RuoYi Harness is an experimental framework that transforms traditional enterprise applications into AI-agent-operable systems.

Instead of users operating software through fixed menus, agents can understand business goals and safely execute enterprise capabilities through a controlled runtime.

## Architecture

```
User
  |
Natural Language Goal
  |
Agent Runtime
  |
Harness Layer
  |-- Tool Registry
  |-- Permission Control
  |-- Workflow Engine
  |-- Audit & Rollback
  |
RuoYi Business Runtime
  |
Database / APIs
```

## Core Concepts

### Tool Registry

Expose business capabilities as structured tools for agents.

Examples:

- query_customer
- create_order
- start_workflow
- generate_report

### Metadata Driven Development

Business requirements can be converted into:

- database schema
- API definitions
- permissions
- UI components

### Human-in-the-loop

High-risk operations require approval before execution.

## Roadmap

- [ ] Project architecture design
- [ ] Agent runtime prototype
- [ ] Tool registry
- [ ] MCP integration
- [ ] RuoYi integration
- [ ] AI generated business modules

## Status

Early research and prototype stage.
