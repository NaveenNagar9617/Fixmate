package com.fixmate.service;

import com.fixmate.dto.complaint.ComplaintFilterRequest;
import com.fixmate.model.Complaint;
import com.fixmate.repository.ComplaintRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// @Service: Spring Boot ko batata hai ki ye ek Business Logic file hai
// @RequiredArgsConstructor: Final variables (repository) ka constructor apne aap bana deta hai
// @Slf4j: Console me logs (messages) print karne ke liye use hota hai
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ComplaintRepository complaintRepository;
    
    // Dates ko Excel me sundar dikhane ke liye format set kiya hai (e.g., 2026-06-22 14:30:00)
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // @Transactional(readOnly = true): Database se sirf data padhna hai, kuch change nahi karna, isse query fast chalti hai
    @Transactional(readOnly = true)
    public byte[] exportComplaintsCSV(ComplaintFilterRequest filter) {
        
        // Pagination setup: CSV me humein saara data ek sath chahiye, isliye page size Integer.MAX_VALUE (unlimited) rakha hai.
        // Sort.Direction.DESC: Nayi complaints sabse upar aayengi.
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Specification: Admin ne frontend par jo bhi filters lagaye hain (Category, Status, Date etc.),
        // ye code un sabko milakar ek Dynamic SQL Query banata hai.
        org.springframework.data.jpa.domain.Specification<Complaint> spec = 
            com.fixmate.repository.spec.ComplaintSpecification.withFilters(
                null, null, filter.getStatus(), filter.getCategory(), 
                filter.getPriority(), filter.getBlock(), filter.getFloor(), 
                filter.getAssignedStaffId(), filter.getFromDate(), 
                filter.getToDate(), filter.getSearch()
        );

        // Database se filtered data manga liya
        Page<Complaint> page = complaintRepository.findAll(spec, pageable);
        List<Complaint> complaints = page.getContent();

        // Try-with-resources: Ye Java ka feature hai. Bracket () ke andar jo bhi streams open hongi,
        // kaam khatam hone ke baad Java unhe apne aap close kar dega (Memory leak nahi hoga).
        try (
             // ByteArrayOutputStream: Data ko Hard disk ki jagah RAM (Memory) me likhne ke liye ek virtual pipe banaya
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             // OutputStreamWriter: Normal text ko UTF-8 format (taki special characters crash na hon) me pipe me bhejta hai
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             // CSVWriter: OpenCSV library ka tool jo array data ko Excel/CSV rows me convert karta hai
             CSVWriter csvWriter = new CSVWriter(osw)
        ) {

            // Step 1: Excel sheet ke columns ki headings (Top Row) set karni hai
            String[] headers = {
                    "ID", "Title", "Category", "Priority", "Status",
                    "Block", "Floor", "Room", "Student", "Assigned Staff",
                    "Created At", "Resolved At", "SLA Deadline", "Resolution Time (hours)"
            };
            csvWriter.writeNext(headers); // Headings ko file me likh diya

            // Step 2: Database se aayi hui har ek complaint par loop chalao
            for (Complaint c : complaints) {
                String resolutionTime = "";
                
                // Agar complaint theek ho chuki hai, toh math calculation karo:
                if (c.getResolvedAt() != null && c.getCreatedAt() != null) {
                    // Complaint aane aur theek hone ke beech kitne ghante (hours) lage, wo nikal liya
                    long hours = Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours();
                    resolutionTime = String.valueOf(hours);
                }

                // Database object (c) se data nikal kar ek String Array (Row) me pack kar rahe hain.
                // Note: null checks (c.getStudent() != null ? ...) lagaye hain taaki NullPointerException aakar server crash na ho.
                String[] row = {
                        c.getId().toString(),
                        c.getTitle(),
                        c.getCategory().name(),
                        c.getPriority().name(),
                        c.getStatus().name(),
                        c.getLocationBlock(),
                        String.valueOf(c.getLocationFloor()),
                        c.getRoomNumber(),
                        c.getStudent() != null ? c.getStudent().getName() : "",
                        c.getAssignedStaff() != null ? c.getAssignedStaff().getName() : "",
                        c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMAT) : "",
                        c.getResolvedAt() != null ? c.getResolvedAt().format(DATE_FORMAT) : "",
                        c.getSlaDeadline() != null ? c.getSlaDeadline().format(DATE_FORMAT) : "",
                        resolutionTime
                };
                // Is pack kiye hue row ko CSV file me likh do
                csvWriter.writeNext(row);
            }

            // Flush: Agar stream pipe me thoda bohot data fasa reh gaya hai, toh use dhakka maarkar aage bhej do
            csvWriter.flush();
            log.info("CSV export generated with {} complaints", complaints.size());
            
            // Sabse important line: Poore RAM pipe me jo data ikattha hua hai, usko Bytes (0 aur 1) 
            // me convert karke return kar do, taki Frontend (React) use download karwa sake.
            return baos.toByteArray();

        } catch (Exception e) {
            // Agar file banane me koi error aayi (jaise memory full ho gayi), toh error log karo aur app ko batao
            log.error("Failed to generate CSV export", e);
            throw new RuntimeException("Failed to generate CSV export: " + e.getMessage(), e);
        }
    }
}

/*abhi ke implementation ke hisaab se Integer.MAX_VALUE database se 5 lakh records ek sath RAM mein load karne ki koshish karega. Isse server par OutOfMemoryError (OOM) aa jayega aur backend crash ho sakta hai.
Ek enterprise application (jaise bade scale par) main isko theek karne ke liye Chunking ya Batch Processing ka use karunga. Main saara data ek sath lane ke bajaye, Page size 10,000 rakhunga, aur ek while loop laga kar 10-10 hazaar records memory pipe mein .flush() karta jaunga, jisse RAM ka use hamesha low rahega. Iske alawa, main is export task ko @Async banakar background mein chala dunga aur Excel ready hone par Admin ko email par link bhej dunga */