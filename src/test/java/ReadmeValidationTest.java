import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;

/**
 * Comprehensive validation tests for README.md and README.zh-CN.md files.
 * These tests ensure documentation quality, consistency, and correctness.
 */
public class ReadmeValidationTest {

    private static String readmeEnContent;
    private static String readmeZhContent;
    private static List<String> readmeEnLines;
    private static List<String> readmeZhLines;
    private static final String README_EN = "README.md";
    private static final String README_ZH = "README.zh-CN.md";

    @BeforeAll
    static void loadReadmeFiles() throws IOException {
        Path readmeEnPath = Paths.get(README_EN);
        Path readmeZhPath = Paths.get(README_ZH);
        
        assertTrue(Files.exists(readmeEnPath), "README.md must exist");
        assertTrue(Files.exists(readmeZhPath), "README.zh-CN.md must exist");
        assertTrue(Files.isReadable(readmeEnPath), "README.md must be readable");
        assertTrue(Files.isReadable(readmeZhPath), "README.zh-CN.md must be readable");
        
        readmeEnContent = Files.readString(readmeEnPath);
        readmeZhContent = Files.readString(readmeZhPath);
        readmeEnLines = Files.readAllLines(readmeEnPath);
        readmeZhLines = Files.readAllLines(readmeZhPath);
        
        assertFalse(readmeEnContent.isEmpty(), "README.md must not be empty");
        assertFalse(readmeZhContent.isEmpty(), "README.zh-CN.md must not be empty");
    }

    // ==================== Structure and Format Tests ====================

    @Test
    @DisplayName("Both READMEs should start with proper H1 title")
    void testH1TitlePresent() {
        assertTrue(readmeEnContent.startsWith("# Flareprox"), 
            "README.md must start with '# Flareprox'");
        assertTrue(readmeZhContent.startsWith("# Flareprox"), 
            "README.zh-CN.md must start with '# Flareprox'");
    }

    @Test
    @DisplayName("Both READMEs should contain GitHub Actions badge")
    void testBadgePresent() {
        String badgePattern = "\\[!\\[Build and Release\\]\\(https://github\\.com/gandli/Flareprox_Burp_Extension/actions/workflows/build\\.yml/badge\\.svg\\)\\]";
        assertTrue(readmeEnContent.contains("[![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)]"),
            "README.md must contain GitHub Actions badge");
        assertTrue(readmeZhContent.contains("[![Build and Release](https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml/badge.svg)]"),
            "README.zh-CN.md must contain GitHub Actions badge");
    }

    @Test
    @DisplayName("Required sections must be present in README.md")
    void testRequiredSectionsEnglish() {
        assertSectionExists(readmeEnContent, "## Overview");
        assertSectionExists(readmeEnContent, "## Core Features");
        assertSectionExists(readmeEnContent, "## Tech Stack & Architecture");
        assertSectionExists(readmeEnContent, "## Requirements");
        assertSectionExists(readmeEnContent, "## Quick Start");
        assertSectionExists(readmeEnContent, "## Cloudflare Worker Templates");
        assertSectionExists(readmeEnContent, "## Troubleshooting");
    }

    @Test
    @DisplayName("Required sections must be present in README.zh-CN.md")
    void testRequiredSectionsChinese() {
        assertSectionExists(readmeZhContent, "## 项目概述");
        assertSectionExists(readmeZhContent, "## 核心功能");
        assertSectionExists(readmeZhContent, "## 技术栈与架构");
        assertSectionExists(readmeZhContent, "## 环境要求");
        assertSectionExists(readmeZhContent, "## 快速开始");
        assertSectionExists(readmeZhContent, "## Cloudflare Worker 模板");
        assertSectionExists(readmeZhContent, "## 故障排查");
    }

    @Test
    @DisplayName("Both READMEs should have parallel section structure")
    void testParallelSectionStructure() {
        // Count H2 sections in both files
        long enSectionCount = readmeEnContent.lines()
            .filter(line -> line.startsWith("## "))
            .count();
        long zhSectionCount = readmeZhContent.lines()
            .filter(line -> line.startsWith("## "))
            .count();
        
        assertEquals(enSectionCount, zhSectionCount, 
            "Both READMEs should have the same number of H2 sections");
        assertEquals(7, enSectionCount, "Should have exactly 7 H2 sections");
    }

    @Test
    @DisplayName("Code blocks must be properly formatted and closed")
    void testCodeBlocksProperlyFormatted() {
        // Test English README
        validateCodeBlocks(readmeEnContent, README_EN);
        // Test Chinese README
        validateCodeBlocks(readmeZhContent, README_ZH);
    }

    @Test
    @DisplayName("All code blocks should specify language (js)")
    void testCodeBlocksHaveLanguage() {
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)");
        
        Matcher enMatcher = codeBlockPattern.matcher(readmeEnContent);
        while (enMatcher.find()) {
            String lang = enMatcher.group(1);
            assertFalse(lang.isEmpty(), 
                "Code blocks in README.md should specify language: " + lang);
            assertEquals("js", lang, 
                "Code blocks in README.md should be JavaScript");
        }
        
        Matcher zhMatcher = codeBlockPattern.matcher(readmeZhContent);
        while (zhMatcher.find()) {
            String lang = zhMatcher.group(1);
            assertFalse(lang.isEmpty(), 
                "Code blocks in README.zh-CN.md should specify language");
            assertEquals("js", lang, 
                "Code blocks in README.zh-CN.md should be JavaScript");
        }
    }

    @Test
    @DisplayName("Both READMEs should contain exactly 2 code blocks (Module and Classic Workers)")
    void testCodeBlockCount() {
        long enCodeBlockCount = countCodeBlocks(readmeEnContent);
        long zhCodeBlockCount = countCodeBlocks(readmeZhContent);
        
        assertEquals(2, enCodeBlockCount, 
            "README.md should contain exactly 2 code blocks");
        assertEquals(2, zhCodeBlockCount, 
            "README.zh-CN.md should contain exactly 2 code blocks");
    }

    // ==================== Content Validation Tests ====================

    @Test
    @DisplayName("README.md should mention correct Java version (21)")
    void testJavaVersionMentionedEnglish() {
        assertTrue(readmeEnContent.contains("Java 21"), 
            "README.md should explicitly mention Java 21");
        assertTrue(readmeEnContent.contains("Java 21+") || readmeEnContent.contains("Java 21 (or newer)"),
            "README.md should indicate Java 21 or newer");
    }

    @Test
    @DisplayName("README.zh-CN.md should mention correct Java version (21)")
    void testJavaVersionMentionedChinese() {
        assertTrue(readmeZhContent.contains("Java 21"), 
            "README.zh-CN.md should explicitly mention Java 21");
    }

    @Test
    @DisplayName("Both READMEs should mention Montoya API")
    void testMontoyaApiMentioned() {
        assertTrue(readmeEnContent.contains("Montoya"), 
            "README.md should mention Montoya API");
        assertTrue(readmeZhContent.contains("Montoya"), 
            "README.zh-CN.md should mention Montoya API");
    }

    @Test
    @DisplayName("Both READMEs should mention key source files")
    void testKeySourceFilesMentioned() {
        // English README
        assertTrue(readmeEnContent.contains("SimpleCloudflareService"), 
            "README.md should mention SimpleCloudflareService");
        assertTrue(readmeEnContent.contains("CloudflareWorkerUtils"), 
            "README.md should mention CloudflareWorkerUtils");
        assertTrue(readmeEnContent.contains("Extension"), 
            "README.md should mention Extension class");
        assertTrue(readmeEnContent.contains("FlareProx"), 
            "README.md should mention FlareProx");
        
        // Chinese README
        assertTrue(readmeZhContent.contains("SimpleCloudflareService"), 
            "README.zh-CN.md should mention SimpleCloudflareService");
        assertTrue(readmeZhContent.contains("CloudflareWorkerUtils"), 
            "README.zh-CN.md should mention CloudflareWorkerUtils");
        assertTrue(readmeZhContent.contains("Extension"), 
            "README.zh-CN.md should mention Extension class");
        assertTrue(readmeZhContent.contains("FlareProx"), 
            "README.zh-CN.md should mention FlareProx");
    }

    @Test
    @DisplayName("Both READMEs should mention build command")
    void testBuildCommandPresent() {
        assertTrue(readmeEnContent.contains("./gradlew build"), 
            "README.md should contain build command");
        assertTrue(readmeZhContent.contains("./gradlew build"), 
            "README.zh-CN.md should contain build command");
    }

    @Test
    @DisplayName("Both READMEs should mention build/libs directory")
    void testBuildLibsDirectoryMentioned() {
        assertTrue(readmeEnContent.contains("build/libs"), 
            "README.md should mention build/libs directory");
        assertTrue(readmeZhContent.contains("build/libs"), 
            "README.zh-CN.md should mention build/libs directory");
    }

    @Test
    @DisplayName("Both READMEs should reference Cloudflare Workers")
    void testCloudflareWorkersMentioned() {
        assertTrue(readmeEnContent.contains("Cloudflare Worker"), 
            "README.md should mention Cloudflare Worker");
        assertTrue(readmeZhContent.contains("Cloudflare Worker"), 
            "README.zh-CN.md should mention Cloudflare Worker");
        
        // Should mention both module and classic workers
        assertTrue(readmeEnContent.contains("module") || readmeEnContent.contains("Module"), 
            "README.md should mention module workers");
        assertTrue(readmeEnContent.contains("classic") || readmeEnContent.contains("Classic"), 
            "README.md should mention classic workers");
        assertTrue(readmeZhContent.contains("模块") || readmeZhContent.contains("module"), 
            "README.zh-CN.md should mention module workers");
        assertTrue(readmeZhContent.contains("经典") || readmeZhContent.contains("classic"), 
            "README.zh-CN.md should mention classic workers");
    }

    // ==================== Cross-Reference Tests ====================

    @Test
    @DisplayName("README.md should reference README.zh-CN.md")
    void testEnglishReadmeReferencesChinese() {
        assertTrue(readmeEnContent.contains("README.zh-CN.md"), 
            "README.md should reference README.zh-CN.md");
        assertTrue(readmeEnContent.contains("Chinese documentation"), 
            "README.md should mention Chinese documentation");
    }

    @Test
    @DisplayName("README.zh-CN.md should reference README.md")
    void testChineseReadmeReferencesEnglish() {
        assertTrue(readmeZhContent.contains("README.md"), 
            "README.zh-CN.md should reference README.md");
        assertTrue(readmeZhContent.contains("英文文档"), 
            "README.zh-CN.md should mention English documentation");
    }

    // ==================== Worker Script Validation Tests ====================

    @Test
    @DisplayName("Module Worker script should be valid JavaScript")
    void testModuleWorkerScriptValidity() {
        String moduleWorkerSection = extractCodeBlock(readmeEnContent, 0);
        assertNotNull(moduleWorkerSection, "Module Worker code block should exist");
        
        // Check for key Module Worker patterns
        assertTrue(moduleWorkerSection.contains("export default"), 
            "Module Worker should contain 'export default'");
        assertTrue(moduleWorkerSection.contains("async fetch(request, env, ctx)"), 
            "Module Worker should have correct fetch signature");
        assertTrue(moduleWorkerSection.contains("X-Target-URL") || 
                   moduleWorkerSection.contains("url"), 
            "Module Worker should handle target URL");
        assertTrue(moduleWorkerSection.contains("new Response"), 
            "Module Worker should create Response objects");
    }

    @Test
    @DisplayName("Classic Worker script should be valid JavaScript")
    void testClassicWorkerScriptValidity() {
        String classicWorkerSection = extractCodeBlock(readmeEnContent, 1);
        assertNotNull(classicWorkerSection, "Classic Worker code block should exist");
        
        // Check for key Classic Worker patterns
        assertTrue(classicWorkerSection.contains("addEventListener"), 
            "Classic Worker should contain 'addEventListener'");
        assertTrue(classicWorkerSection.contains("async function handleRequest"), 
            "Classic Worker should have handleRequest function");
        assertTrue(classicWorkerSection.contains("url"), 
            "Classic Worker should handle URL parameter");
        assertTrue(classicWorkerSection.contains("new Response"), 
            "Classic Worker should create Response objects");
    }

    @Test
    @DisplayName("Both Worker scripts should handle missing target URL gracefully")
    void testWorkerErrorHandling() {
        String moduleWorker = extractCodeBlock(readmeEnContent, 0);
        String classicWorker = extractCodeBlock(readmeEnContent, 1);
        
        assertTrue(moduleWorker.contains("error") || moduleWorker.contains("No target URL"), 
            "Module Worker should handle missing URL error");
        assertTrue(classicWorker.contains("error") || classicWorker.contains("No target URL"), 
            "Classic Worker should handle missing URL error");
        
        assertTrue(moduleWorker.contains("400"), 
            "Module Worker should return 400 status for missing URL");
        assertTrue(classicWorker.contains("400"), 
            "Classic Worker should return 400 status for missing URL");
    }

    @Test
    @DisplayName("Both Worker scripts should set CORS headers")
    void testWorkerCorsHeaders() {
        String moduleWorker = extractCodeBlock(readmeEnContent, 0);
        String classicWorker = extractCodeBlock(readmeEnContent, 1);
        
        assertTrue(moduleWorker.contains("Access-Control-Allow-Origin"), 
            "Module Worker should set CORS Origin header");
        assertTrue(moduleWorker.contains("Access-Control-Allow-Headers"), 
            "Module Worker should set CORS Headers header");
        
        assertTrue(classicWorker.contains("Access-Control-Allow-Origin"), 
            "Classic Worker should set CORS Origin header");
        assertTrue(classicWorker.contains("Access-Control-Allow-Headers"), 
            "Classic Worker should set CORS Headers header");
    }

    @Test
    @DisplayName("Worker scripts in both READMEs should be identical")
    void testWorkerScriptConsistency() {
        String enModuleWorker = extractCodeBlock(readmeEnContent, 0);
        String zhModuleWorker = extractCodeBlock(readmeZhContent, 0);
        String enClassicWorker = extractCodeBlock(readmeEnContent, 1);
        String zhClassicWorker = extractCodeBlock(readmeZhContent, 1);
        
        assertEquals(normalizeWhitespace(enModuleWorker), 
                     normalizeWhitespace(zhModuleWorker), 
            "Module Worker scripts should be identical in both READMEs");
        assertEquals(normalizeWhitespace(enClassicWorker), 
                     normalizeWhitespace(zhClassicWorker), 
            "Classic Worker scripts should be identical in both READMEs");
    }

    // ==================== Link Validation Tests ====================

    @Test
    @DisplayName("GitHub Actions badge link should be properly formatted")
    void testBadgeLinkFormat() {
        String expectedBadgeUrl = "https://github.com/gandli/Flareprox_Burp_Extension/actions/workflows/build.yml";
        assertTrue(readmeEnContent.contains(expectedBadgeUrl), 
            "README.md should contain correct badge link");
        assertTrue(readmeZhContent.contains(expectedBadgeUrl), 
            "README.zh-CN.md should contain correct badge link");
    }

    @Test
    @DisplayName("No broken internal file references")
    void testInternalFileReferences() {
        // Check that cross-referenced files actually exist
        assertTrue(Files.exists(Paths.get(README_EN)), 
            "Referenced README.md should exist");
        assertTrue(Files.exists(Paths.get(README_ZH)), 
            "Referenced README.zh-CN.md should exist");
        assertTrue(Files.exists(Paths.get("build.gradle.kts")), 
            "Referenced build.gradle.kts should exist");
    }

    // ==================== Formatting and Style Tests ====================

    @Test
    @DisplayName("No trailing whitespace on lines")
    void testNoTrailingWhitespace() {
        for (int i = 0; i < readmeEnLines.size(); i++) {
            String line = readmeEnLines.get(i);
            if (!line.isEmpty()) {
                assertFalse(line.endsWith(" ") || line.endsWith("\t"), 
                    "README.md line " + (i + 1) + " should not have trailing whitespace");
            }
        }
        
        for (int i = 0; i < readmeZhLines.size(); i++) {
            String line = readmeZhLines.get(i);
            if (!line.isEmpty()) {
                assertFalse(line.endsWith(" ") || line.endsWith("\t"), 
                    "README.zh-CN.md line " + (i + 1) + " should not have trailing whitespace");
            }
        }
    }

    @Test
    @DisplayName("Numbered lists should use consistent format")
    void testNumberedListFormat() {
        Pattern numberedListPattern = Pattern.compile("^\\d+\\. ");
        
        for (int i = 0; i < readmeEnLines.size(); i++) {
            String line = readmeEnLines.get(i);
            if (numberedListPattern.matcher(line).find()) {
                // Check that the line doesn't have inconsistent spacing
                assertFalse(line.matches("^\\d+\\.  .*"), 
                    "README.md line " + (i + 1) + " should have single space after number");
            }
        }
    }

    @Test
    @DisplayName("Inline code should use backticks consistently")
    void testInlineCodeFormatting() {
        // Check for key technical terms that should be in backticks
        String[] technicalTerms = {"Burp Suite", "Cloudflare", "build/libs", 
                                   "Extender", "Extensions", "Java"};
        
        for (String term : technicalTerms) {
            if (term.equals("Burp Suite") || term.equals("Cloudflare")) {
                // These are product names, often not in backticks
                continue;
            }
            // Count occurrences in backticks vs plain text
            int backtickedEn = countOccurrences(readmeEnContent, "`" + term + "`");
            int plainEn = countOccurrences(readmeEnContent, term) - backtickedEn;
            // Most technical paths/commands should be in backticks
            if (term.contains("/")) {
                assertTrue(backtickedEn > 0, 
                    "Path '" + term + "' should appear in backticks in README.md");
            }
        }
    }

    @Test
    @DisplayName("Section headers should have proper spacing")
    void testSectionHeaderSpacing() {
        for (int i = 1; i < readmeEnLines.size(); i++) {
            String line = readmeEnLines.get(i);
            if (line.startsWith("## ")) {
                String prevLine = readmeEnLines.get(i - 1);
                assertTrue(prevLine.isEmpty() || prevLine.startsWith("#"), 
                    "README.md section header at line " + (i + 1) + 
                    " should have blank line before it");
            }
        }
    }

    // ==================== Content Completeness Tests ====================

    @Test
    @DisplayName("Quick Start section should have step-by-step instructions")
    void testQuickStartCompleteness() {
        // Find Quick Start section
        int startIdx = findSectionStart(readmeEnLines, "## Quick Start");
        assertTrue(startIdx >= 0, "Quick Start section should exist");
        
        // Should contain numbered steps
        boolean hasNumberedSteps = false;
        for (int i = startIdx; i < Math.min(startIdx + 20, readmeEnLines.size()); i++) {
            if (readmeEnLines.get(i).matches("^\\d+\\..*")) {
                hasNumberedSteps = true;
                break;
            }
        }
        assertTrue(hasNumberedSteps, "Quick Start should contain numbered steps");
    }

    @Test
    @DisplayName("Requirements section should list all prerequisites")
    void testRequirementsCompleteness() {
        String reqSection = extractSection(readmeEnContent, "## Requirements");
        assertNotNull(reqSection, "Requirements section should exist");
        
        assertTrue(reqSection.contains("Burp Suite"), 
            "Requirements should mention Burp Suite");
        assertTrue(reqSection.contains("Java"), 
            "Requirements should mention Java");
        assertTrue(reqSection.contains("Cloudflare"), 
            "Requirements should mention Cloudflare account");
        assertTrue(reqSection.contains("API token") || reqSection.contains("token"), 
            "Requirements should mention API token");
    }

    @Test
    @DisplayName("Troubleshooting section should provide actionable guidance")
    void testTroubleshootingCompleteness() {
        String troubleshootingEn = extractSection(readmeEnContent, "## Troubleshooting");
        String troubleshootingZh = extractSection(readmeZhContent, "## 故障排查");
        
        assertNotNull(troubleshootingEn, "English troubleshooting section should exist");
        assertNotNull(troubleshootingZh, "Chinese troubleshooting section should exist");
        
        // Should mention common issues
        assertTrue(troubleshootingEn.contains("Java") || troubleshootingEn.contains("version"), 
            "Troubleshooting should address Java version issues");
        assertTrue(troubleshootingEn.contains("API token") || troubleshootingEn.contains("token"), 
            "Troubleshooting should address API token issues");
    }

    // ==================== Helper Methods ====================

    private void assertSectionExists(String content, String sectionHeader) {
        assertTrue(content.contains(sectionHeader), 
            "Section '" + sectionHeader + "' must exist");
    }

    private void validateCodeBlocks(String content, String filename) {
        long openCount = content.lines()
            .filter(line -> line.startsWith("```"))
            .count();
        assertTrue(openCount % 2 == 0, 
            filename + " must have even number of code block markers (each block must be closed)");
    }

    private long countCodeBlocks(String content) {
        return content.lines()
            .filter(line -> line.startsWith("```"))
            .count() / 2;
    }

    private String extractCodeBlock(String content, int index) {
        List<String> lines = content.lines().toList();
        int blockCount = 0;
        int startIdx = -1;
        
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("```")) {
                if (startIdx == -1) {
                    if (blockCount == index) {
                        startIdx = i + 1;
                    }
                    blockCount++;
                } else {
                    if (blockCount - 1 == index) {
                        List<String> codeLines = lines.subList(startIdx, i);
                        return String.join("\n", codeLines);
                    }
                    startIdx = -1;
                }
            }
        }
        return null;
    }

    private String normalizeWhitespace(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s+", " ").trim();
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private int findSectionStart(List<String> lines, String sectionHeader) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(sectionHeader)) {
                return i;
            }
        }
        return -1;
    }

    private String extractSection(String content, String sectionHeader) {
        List<String> lines = content.lines().toList();
        int startIdx = findSectionStart(lines, sectionHeader);
        if (startIdx < 0) return null;
        
        StringBuilder section = new StringBuilder();
        for (int i = startIdx + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("##")) break;
            section.append(line).append("\n");
        }
        return section.toString();
    }

    // Main method for standalone execution (following existing test pattern)
    public static void main(String[] args) throws Exception {
        ReadmeValidationTest test = new ReadmeValidationTest();
        loadReadmeFiles();
        
        System.out.println("Running README validation tests...");
        int passed = 0;
        int failed = 0;
        
        // Run all tests
        try { test.testH1TitlePresent(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testH1TitlePresent - " + e.getMessage()); }
        
        try { test.testBadgePresent(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testBadgePresent - " + e.getMessage()); }
        
        try { test.testRequiredSectionsEnglish(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testRequiredSectionsEnglish - " + e.getMessage()); }
        
        try { test.testRequiredSectionsChinese(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testRequiredSectionsChinese - " + e.getMessage()); }
        
        try { test.testParallelSectionStructure(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testParallelSectionStructure - " + e.getMessage()); }
        
        try { test.testCodeBlocksProperlyFormatted(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testCodeBlocksProperlyFormatted - " + e.getMessage()); }
        
        try { test.testCodeBlocksHaveLanguage(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testCodeBlocksHaveLanguage - " + e.getMessage()); }
        
        try { test.testCodeBlockCount(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testCodeBlockCount - " + e.getMessage()); }
        
        try { test.testJavaVersionMentionedEnglish(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testJavaVersionMentionedEnglish - " + e.getMessage()); }
        
        try { test.testJavaVersionMentionedChinese(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testJavaVersionMentionedChinese - " + e.getMessage()); }
        
        try { test.testMontoyaApiMentioned(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testMontoyaApiMentioned - " + e.getMessage()); }
        
        try { test.testKeySourceFilesMentioned(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testKeySourceFilesMentioned - " + e.getMessage()); }
        
        try { test.testBuildCommandPresent(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testBuildCommandPresent - " + e.getMessage()); }
        
        try { test.testBuildLibsDirectoryMentioned(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testBuildLibsDirectoryMentioned - " + e.getMessage()); }
        
        try { test.testCloudflareWorkersMentioned(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testCloudflareWorkersMentioned - " + e.getMessage()); }
        
        try { test.testEnglishReadmeReferencesChinese(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testEnglishReadmeReferencesChinese - " + e.getMessage()); }
        
        try { test.testChineseReadmeReferencesEnglish(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testChineseReadmeReferencesEnglish - " + e.getMessage()); }
        
        try { test.testModuleWorkerScriptValidity(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testModuleWorkerScriptValidity - " + e.getMessage()); }
        
        try { test.testClassicWorkerScriptValidity(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testClassicWorkerScriptValidity - " + e.getMessage()); }
        
        try { test.testWorkerErrorHandling(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testWorkerErrorHandling - " + e.getMessage()); }
        
        try { test.testWorkerCorsHeaders(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testWorkerCorsHeaders - " + e.getMessage()); }
        
        try { test.testWorkerScriptConsistency(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testWorkerScriptConsistency - " + e.getMessage()); }
        
        try { test.testBadgeLinkFormat(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testBadgeLinkFormat - " + e.getMessage()); }
        
        try { test.testInternalFileReferences(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testInternalFileReferences - " + e.getMessage()); }
        
        try { test.testNoTrailingWhitespace(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testNoTrailingWhitespace - " + e.getMessage()); }
        
        try { test.testNumberedListFormat(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testNumberedListFormat - " + e.getMessage()); }
        
        try { test.testInlineCodeFormatting(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testInlineCodeFormatting - " + e.getMessage()); }
        
        try { test.testSectionHeaderSpacing(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testSectionHeaderSpacing - " + e.getMessage()); }
        
        try { test.testQuickStartCompleteness(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testQuickStartCompleteness - " + e.getMessage()); }
        
        try { test.testRequirementsCompleteness(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testRequirementsCompleteness - " + e.getMessage()); }
        
        try { test.testTroubleshootingCompleteness(); passed++; } 
        catch (AssertionError e) { failed++; System.err.println("FAIL: testTroubleshootingCompleteness - " + e.getMessage()); }
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("README Validation Test Results:");
        System.out.println("  Passed: " + passed);
        System.out.println("  Failed: " + failed);
        System.out.println("  Total:  " + (passed + failed));
        System.out.println("=".repeat(50));
        
        if (failed > 0) {
            System.exit(1);
        }
    }
}