# Agent Instructions - Luna Fetch

## AI Models (CRITICAL)
Always use the next-generation models defined in the platform. Do NOT use legacy models like Gemini 1.5 or old GPT versions unless explicitly requested for legacy testing.

**Current Recommended Models (2026):**
- `gemini-3.5-flash` (Default for general chat/intelligence / Smart)
- `gemini-3.1-flash-lite` (Fast responses / G-3.1 Flash)
- `gemini-3.1-pro-preview` (Deep reasoning / Complex tasks / G-3.1 Pro)

## Project License & Author
- **License**: MIT
- **Author**: Biglex J (2026)

## Reference Project (Golden Standard)
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código o patrones de documentación, consulta el proyecto **Aurora Blog**:
- **Raíz del Proyecto**: `d:\Proyectos\biglexj\Aurora---Blog` (especialmente su archivo [agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md))
- **Documentación del Proyecto**: [docs](file:///d:/Proyectos/biglexj/Aurora---Blog/docs) (incluyendo la guía de diseño en [DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md) y la estructura de directorios en [Arbol de Carpetas.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md))

## Estructura de Carpetas de Trabajo (Scratch & Test)
- **Uso de `scratch/`**:
  - Solo se utiliza en la raíz del proyecto para scripts utilitarios, tareas de mantenimiento o migraciones.
  - Queda estrictamente prohibido crear carpetas `scratch/` dentro de subdirectorios de código.
  - La carpeta `scratch/` debe mantenerse limpia y organizada por categorías. No se deben dejar archivos sueltos en la raíz de `scratch/`.
- **Uso de `test/`**:
  - Cualquier script de prueba temporal, simulaciones o pruebas del entorno de desarrollo (como pruebas de generación de archivos) debe crearse dentro de la carpeta `test/` en la raíz.
  - La carpeta `test/` está ignorada en `.gitignore` para evitar que se suban archivos temporales al repositorio de Git.

## Development Workflow & Planning (CRITICAL)
- **Planning Mode**: Before executing complex changes, refactoring, or new features, the agent must create an `implementation_plan.md` in the task context or workspace and wait for the user's approval.
- **Task Tracking**: Once approved, create `task.md` to track progress of task checklists.
- **Verification**: Always verify code builds, and run unit tests or manual tests to verify code. Use `walkthrough.md` to document changes made.

## Documentation Maintenance Rules
The agent must keep documentation clean and updated according to the following guidelines:

### 1. ROADMAP.md
- **Urgente / Importante**: Tareas críticas, corrección de errores, requerimientos indispensables para el hito actual.
- **Intermedio**: Tareas secundarias, mejoras de rendimiento o funcionalidades opcionales.
- **Completado**: Historial limpio de tareas finalizadas.
- Mantener descripciones claras, concisas y estructuradas.

### 2. RELEASE_NOTES.md
- **Límite de Extensión (CRÍTICO)**: No escribir registros de versión demasiado largos. Para parches pequeños, 1-2 párrafos (promedio de 3 líneas por párrafo) son suficientes. Para lanzamientos mayores, escribir un máximo de 4-5 párrafos. Evitar listas detalladas de archivos.
- **No duplicar versiones**: Si una versión ya está registrada localmente pero aún no se ha hecho push a Git, añadir los nuevos cambios bajo la misma versión activa en lugar de crear una nueva versión de parche.
- **Límite de Parches (Regla del .9)**: Nunca pasar de una versión de parche `.9` (por ejemplo, de `1.0.9` pasar a `1.1.0` en lugar de `1.0.10`).
- **Nombres de Dulces**: Cada versión mayor (ej: `2.0.0`) debe nombrarse con un dulce o postre (estilo Android) y estar coordinado en `gradle.properties`, `README.md` y `RELEASE_NOTES.md`.

### 3. RELEASE_MESSAGE.md
- Usar un formato conciso, limpio y con emojis para anunciar el lanzamiento a usuarios o canales de chat.
- Estructura:
  - Título y Versión con emojis.
  - Resumen rápido del lanzamiento.
  - Novedades destacadas (lista corta con viñetas).
