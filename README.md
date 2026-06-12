# Supplog

Supplog is a health routine tracking application designed to help users manage medicines, vitamins, supplements, and personal reminders in one place.

The idea for this project came from a personal challenge: remembering to take medications and supplements consistently. While building a solution for my own routine, I also wanted to gain hands-on experience with modern backend development technologies and software architecture principles.

This repository currently contains the backend application built with Spring Boot and PostgreSQL. The frontend application is under development.

---

## Project Goals

* Manage medicines, vitamins, supplements, and reminders
* Create personalized routines
* Track daily adherence and consistency
* Build a supporter system for accountability and motivation
* Gain practical experience with full-stack application development

---

## Current Features

### User Management

* Create user
* Get user by id
* Get user by email
* Get user by username
* Update user profile
* Change password
* Soft delete user

### Supplement Management

* Create supplement
* Get supplement by id
* Get supplements by user
* Update supplement information
* Update supplement dosage
* Soft delete supplement

### Routine Management

* Create routine
* Get routines by user
* Update routine time
* Update routine day
* Update routine period
* Soft delete routine

---

## Technologies

### Backend

* Java
* Spring Boot
* Spring Data JPA
* Hibernate
* PostgreSQL
* Maven
* ModelMapper
* Lombok

### Concepts Practiced

* Layered Architecture
* RESTful API Design
* DTO Pattern
* Entity Relationships
* Soft Delete
* Dependency Injection
* Repository Pattern
* Service Layer Pattern
* CRUD Operations

---

## Database Relationships

```text
User
 ├── Supplements (One-to-Many)
 └── Routines (One-to-Many)

Supplement
 └── Routine (One-to-One)
```

---

## Project Structure

```text
supplog
├── src
├── pom.xml
├── mvnw
└── README.md
```

---

## Roadmap

Planned features:

* JWT Authentication
* Google Login
* Facebook Login
* Notification System
* Supporter System
* Routine History Tracking
* Statistics Dashboard
* Achievement & Badge System
* React Frontend
* React Native Mobile Application

---

## Learning Objectives

This project is being developed to improve my skills in:

* Spring Boot
* JPA & Hibernate
* PostgreSQL
* API Design
* Software Architecture
* Authentication & Authorization
* React & TypeScript
* Mobile Development

---

## Project Status

🚧 Active Development

Completed modules:

* User Module
* Supplement Module
* Routine Module

Current focus:

* React Frontend Development

---

Developed by Muhammed Emre Eger
