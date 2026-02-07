# IntelliJ IDEA Configuration Guide for Cognify Backend

## 🎯 Problem: Java Files Show "0101" Icon Instead of Java Icon

This indicates IntelliJ doesn't recognize your project structure correctly. Follow these steps:

---

## ✅ Solution 1: Reload Maven Project

### Step 1: Open Maven Tool Window
1. Go to **View** → **Tool Windows** → **Maven**
2. Or press **Ctrl+Shift+M** (Windows/Linux) or **Cmd+Shift+M** (Mac)

### Step 2: Reload All Maven Projects
1. In the Maven tool window, click the **Reload All Maven Projects** button (🔄 icon at the top)
2. Wait for dependencies to download (check progress at bottom of IDE)

### Step 3: Verify
- Java files should now show the Java icon (☕)
- No red underlines on imports

---

## ✅ Solution 2: Invalidate Caches and Restart

If reload doesn't work:

1. Go to **File** → **Invalidate Caches...**
2. In the dialog, check:
   - ✅ **Invalidate and Restart**
   - ✅ **Clear file system cache and Local History**
   - ✅ **Clear VCS Log caches and indexes**
3. Click **Invalidate and Restart**
4. Wait for IntelliJ to restart and re-index

---

## ✅ Solution 3: Mark Directories Correctly

### Verify Source Folders:
1. Right-click on `src/main/java` folder
2. Select **Mark Directory as** → **Sources Root** (should already be blue)

3. Right-click on `src/main/resources` folder
4. Select **Mark Directory as** → **Resources Root** (should have a folder icon with a dot)

5. Right-click on `src/test/java` folder
6. Select **Mark Directory as** → **Test Sources Root** (should be green)

### Remove Invalid Source Folder:
❌ **IMPORTANT**: There's a file `src/main/resources/CognifyBackendApplication.java` that shouldn't exist!
- Java files should NEVER be in resources folder
- This is likely causing confusion
- The correct file is at: `src/main/java/com/cognify/cognify_backend/CognifyBackendApplication.java`

---

## ✅ Solution 4: Enable Lombok Plugin & Annotation Processing

### Step 1: Install Lombok Plugin
1. Go to **File** → **Settings** (Ctrl+Alt+S)
2. Navigate to **Plugins**
3. Search for "**Lombok**"
4. Click **Install** (if not already installed)
5. Restart IntelliJ if prompted

### Step 2: Enable Annotation Processing
1. Go to **File** → **Settings** (Ctrl+Alt+S)
2. Navigate to: **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
3. Check: ✅ **Enable annotation processing**
4. Set:
   - Processor path: **Use classpath of module**
   - Store generated sources relative to: **Module content root**
   - Production sources directory: `target/generated-sources/annotations`
   - Test sources directory: `target/generated-test-sources/test-annotations`
5. Click **Apply** and **OK**

---

## ✅ Solution 5: Configure Java SDK

### Verify Java Version:
1. Go to **File** → **Project Structure** (Ctrl+Alt+Shift+S)
2. Select **Project** from left sidebar
3. Ensure:
   - **SDK**: Java 17 or higher
   - **Language Level**: 17 - Sealed types, always-strict floating-point semantics
4. Click **Apply**

### Check Module SDK:
1. Still in **Project Structure**
2. Select **Modules** from left sidebar
3. Click on **cognify-backend** module
4. Ensure **Module SDK** is set to Java 17+

---

## ✅ Solution 6: Reimport Maven Project from Scratch

If nothing else works:

### Step 1: Close Project
1. **File** → **Close Project**

### Step 2: Delete IDE Configuration
Navigate to your project folder and delete:
- `.idea` folder
- `*.iml` files

### Step 3: Reimport
1. Open IntelliJ
2. Click **Open**
3. Navigate to your project folder
4. Select the **pom.xml** file
5. Choose **Open as Project**
6. In the dialog, select **Open as Project** (NOT "Import")
7. Wait for indexing and Maven sync

---

## 🔧 Additional IntelliJ Configuration

### Enable Auto-Import for Maven
1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**
2. Check: ✅ **Import Maven projects automatically**
3. Check: ✅ **Automatically download: Sources** and **Documentation**

### Configure Maven in IntelliJ
1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. Set **Maven home path**: Bundled (Maven 3)
3. Set **User settings file**: Use default (~/.m2/settings.xml)
4. Set **Local repository**: Use default (~/.m2/repository)

### Build Configuration
1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
2. Set **Build process heap size**: 2048 MB (for large projects)
3. Check: ✅ **Compile independent modules in parallel**

---

## 🐛 Troubleshooting Specific Issues

### Issue: "Cannot resolve symbol" for Spring annotations
**Solution**: Ensure Maven imported Spring Boot dependencies correctly
```
File → Settings → Build, Execution, Deployment → Build Tools → Maven → Importing
✅ Import Maven projects automatically
```

### Issue: Lombok annotations not recognized (@Data, @Builder, etc.)
**Solution**: 
1. Install Lombok plugin (Solution 4)
2. Enable annotation processing (Solution 4)
3. Rebuild project: **Build** → **Rebuild Project**

### Issue: Red underlines on valid code
**Solution**:
1. **File** → **Invalidate Caches** → **Invalidate and Restart**
2. Wait for re-indexing

### Issue: Maven dependencies not downloading
**Solution**:
1. Check internet connection
2. Clear Maven cache:
   ```powershell
   rm -Recurse -Force ~/.m2/repository/org/springframework
   ```
3. Reload Maven project

### Issue: "Module not specified" error when running
**Solution**:
1. Go to **Run** → **Edit Configurations**
2. Click on **CognifyBackendApplication**
3. Set **Module**: cognify-backend
4. Set **Main class**: com.cognify.cognify_backend.CognifyBackendApplication
5. Set **Working directory**: Project root

---

## 🚀 Creating Run Configuration

### Spring Boot Run Configuration:
1. **Run** → **Edit Configurations**
2. Click **+** → **Spring Boot**
3. Configure:
   - **Name**: Cognify Backend
   - **Main class**: com.cognify.cognify_backend.CognifyBackendApplication
   - **Program arguments**: (leave empty)
   - **VM options**: `-Xmx2048m -Xms512m`
   - **Environment variables**: (add if needed)
   - **Working directory**: $MODULE_WORKING_DIR$
4. Click **Apply** and **OK**

### Run the Application:
- Click the green **Run** button (▶️)
- Or press **Shift+F10**

---

## ✨ Recommended IntelliJ Plugins

1. **Lombok** - Required for @Data, @Builder, etc.
2. **Spring Boot Assistant** - Better Spring support
3. **Maven Helper** - Analyze dependencies
4. **Rainbow Brackets** - Better code readability
5. **Key Promoter X** - Learn shortcuts

---

## 📋 Quick Checklist

Before running the project, ensure:
- ✅ Maven project reloaded
- ✅ All dependencies downloaded (check Maven tool window)
- ✅ No red underlines on imports
- ✅ Java files show Java icon (not 0101)
- ✅ Lombok plugin installed
- ✅ Annotation processing enabled
- ✅ Java 17 SDK configured
- ✅ PostgreSQL running on localhost:5432
- ✅ Redis running on localhost:6379
- ✅ Database 'cognify' exists

---

## 🆘 Still Not Working?

1. Check IntelliJ logs: **Help** → **Show Log in Explorer**
2. Check Maven output in IDE console
3. Try running via terminal: `.\mvnw.cmd spring-boot:run`
4. Update IntelliJ to latest version
5. Update Maven wrapper: `.\mvnw.cmd wrapper:wrapper`

---

**Last Updated**: February 2026  
**IntelliJ Version**: 2025.2+ recommended  
**Status**: ✅ Configuration Complete
