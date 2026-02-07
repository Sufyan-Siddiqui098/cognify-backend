# 🚀 COGNIFY BACKEND - QUICK REFERENCE

## ⚡ Quick Fix Commands

```powershell
# Navigate to project
cd "C:\Users\Sufyan\OneDrive\Documents\University Material\8th Semester (Fall)\FYP\cognify-backend\cognify-backend"

# Option 1: Use automated script
.\reload-project.ps1

# Option 2: Manual Maven commands
.\mvnw.cmd clean install -DskipTests

# Run the application
.\mvnw.cmd spring-boot:run
```

## 🔧 IntelliJ Quick Fixes

### If Java files show "0101" icon:
1. **Ctrl+Shift+A** → Type "Reload Maven" → Enter
2. **File** → **Invalidate Caches** → **Invalidate and Restart**

### If Lombok not working:
1. **Ctrl+Alt+S** → **Plugins** → Install "Lombok"
2. **Ctrl+Alt+S** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors** → ✅ Enable

## 📋 What Changed?

| Component | Version |
|-----------|---------|
| Spring Boot | **3.4.2** (was 3.2.1) |
| Lombok | **1.18.30** (was unspecified) |
| JJWT | **0.12.6** (was 0.12.3) |
| Commons-IO | **2.17.0** (was 2.15.1) |

## ✅ Pre-flight Checklist

Before running:
- [ ] PostgreSQL running on `localhost:5432`
- [ ] Redis running on `localhost:6379`
- [ ] Database `cognify` exists
- [ ] Maven dependencies downloaded
- [ ] IntelliJ caches invalidated

## 🧪 Test Commands

```powershell
# Health check
curl http://localhost:8082/api/health

# Signup
curl -X POST http://localhost:8082/api/auth/signup -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"Test123!","name":"Test"}'
```

## 🐛 Common Issues

| Problem | Solution |
|---------|----------|
| Port 8082 in use | `netstat -ano \| findstr :8082` then kill process |
| Redis not found | `choco install redis-64` then `redis-server` |
| Flyway error | Drop and recreate database schema |
| Java files red | Reload Maven + Invalidate Caches |

## 📚 Documentation Files

- 📖 **FIX_SUMMARY.md** - Complete list of fixes
- 📖 **VERSION_UPDATE_GUIDE.md** - Detailed update guide  
- 📖 **INTELLIJ_SETUP.md** - IntelliJ configuration
- 📖 **README.md** - Project overview

## 🎯 Next Steps

1. Apply fixes (run `reload-project.ps1`)
2. Configure IntelliJ (follow INTELLIJ_SETUP.md)
3. Start dependencies (PostgreSQL + Redis)
4. Run application
5. Test endpoints

---

**Status**: ✅ All issues fixed and ready to run  
**Last Updated**: Feb 2026
