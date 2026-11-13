# GitHub Repository Analyzer — Documentation Index

This directory contains comprehensive documentation for the GitHub Repository Analyzer project.

---

## 📚 Documentation Files

### API Documentation
- **[OpenAPI Specification](openapi.yaml)** — Complete API contract in OpenAPI 3.0 format
  - Importable into Postman, Insomnia, or any OpenAPI-compatible tool
  - Includes request/response schemas, examples, and error codes
  - **Live Swagger UI**: http://localhost:8080/swagger-ui.html (when backend is running)

### Architecture & Design  
See the [`architecture/`](../architecture/) directory for:

| Document | Description |
|----------|-------------|
| **[00_INDEX.md](../architecture/00_INDEX.md)** | Master index of all architecture docs |
| **[01_HLD_System_Overview.md](../architecture/01_HLD_System_Overview.md)** | High-level design, system components, deployment |
| **[02_Architecture_Decision_Records.md](../architecture/02_Architecture_Decision_Records.md)** | Why Option A was chosen, technology decisions |
| **[03_Sequence_Diagrams.md](../architecture/03_Sequence_Diagrams.md)** | PlantUML sequence diagrams for key flows |
| **[04_UML_Class_Diagrams.md](../architecture/04_UML_Class_Diagrams.md)** | Backend class structure and relationships |
| **[05_React_Component_Design.md](../architecture/05_React_Component_Design.md)** | Frontend component tree and state management |
| **[06_API_Contract.md](../architecture/06_API_Contract.md)** | Detailed API contract (also in openapi.yaml) |
| **[07_Cache_And_Resilience_Design.md](../architecture/07_Cache_And_Resilience_Design.md)** | Caching strategy, TTL, degradation tiers |
| **[08_CI_CD_Pipeline_Design.md](../architecture/08_CI_CD_Pipeline_Design.md)** | Jenkins & GitHub Actions pipeline design |
| **[09_Design_Patterns_Used.md](../architecture/09_Design_Patterns_Used.md)** | 12 patterns with implementation details |
| **[10_Concepts_Reference.md](../architecture/10_Concepts_Reference.md)** | Technical concept refresher |
| **[11_Project_Folder_Structure.md](../architecture/11_Project_Folder_Structure.md)** | Directory structure explanation |

### Sequence Diagrams
- **[sequence-diagrams.drawio](sequence-diagrams.drawio)** — Draw.io compatible diagrams
  - Open with https://app.diagrams.net/
  - Contains all major user flows
  - Same diagrams as PlantUML version in architecture docs

### Frontend Learning Guides
See the [`FRONTEND_LEARNING/`](../FRONTEND_LEARNING/) directory for:
- Step-by-step code walkthroughs
- Component hierarchy
- Data flow maps
- Quick reference cheatsheet

---

## 🎯 Documentation Requirements Compliance

Per the case study requirements:

| Requirement | Status | Location |
|-------------|--------|----------|
| ✅ **OpenAPI/Swagger specification** | COMPLETE | `docs/openapi.yaml` + Live at `/swagger-ui.html` |
| ✅ **README.md with design approach** | COMPLETE | Root `README.md` |
| ✅ **Sequence diagram (draw.io)** | COMPLETE | `docs/sequence-diagrams.drawio` + `architecture/03_Sequence_Diagrams.md` (PlantUML) |
| ✅ **Design patterns documentation** | COMPLETE | `architecture/09_Design_Patterns_Used.md` |

---

## 🚀 Quick Access Links

### For Developers
- **[Main README](../README.md)** — Start here
- **[Running Guide](../RUNNING_GUIDE.md)** — How to run locally
- **[Backend Guide](../BACKEND_GUIDE.md)** — Backend-specific details

### For Reviewers / Promotion Panel
1. **System Overview**: [`architecture/01_HLD_System_Overview.md`](../architecture/01_HLD_System_Overview.md)
2. **Design Decisions**: [`architecture/02_Architecture_Decision_Records.md`](../architecture/02_Architecture_Decision_Records.md)
3. **Design Patterns**: [`architecture/09_Design_Patterns_Used.md`](../architecture/09_Design_Patterns_Used.md)
4. **API Contract**: [`docs/openapi.yaml`](openapi.yaml)
5. **Live Demo**: 
   - Frontend: https://github-analyzer-ui-niksoni.azurewebsites.net
   - Backend: https://github-analyzer-api-niksoni.azurewebsites.net
   - Swagger: https://github-analyzer-api-niksoni.azurewebsites.net/swagger-ui.html

### For Testing
- **Swagger UI**: Interactive API testing
  - Local: http://localhost:8080/swagger-ui.html
  - Production: https://github-analyzer-api-niksoni.azurewebsites.net/swagger-ui.html
- **Postman**: Import `docs/openapi.yaml`
- **Sample Request**:
  ```bash
  curl "http://localhost:8080/api/analyze?owner=facebook&repo=react"
  ```

---

## 📖 Documentation Standards

All documentation follows these principles:

1. **Accuracy** — Code examples are tested and up-to-date
2. **Completeness** — Architecture decisions are explained, not just stated
3. **Accessibility** — Multiple formats (Markdown, PlantUML, Draw.io, OpenAPI)
4. **Traceability** — Requirements map to implementation
5. **Professional** — Suitable for stakeholder presentation

---

## 🔄 Keeping Docs Updated

When making changes:

1. **Code changes** → Update relevant architecture docs
2. **API changes** → Update `openapi.yaml` and regenerate Swagger
3. **New patterns** → Add to `09_Design_Patterns_Used.md`
4. **New flows** → Add sequence diagram to both PlantUML and Draw.io versions

---

## 📞 Contact

For questions about the documentation:
- Create an issue in the repository
- Contact: support@example.com

---

**Last Updated**: 2026-06-15  
**Documentation Version**: 1.0.0
