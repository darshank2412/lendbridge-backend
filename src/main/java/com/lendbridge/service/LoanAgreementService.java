package com.lendbridge.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.lendbridge.entity.EmiSchedule;
import com.lendbridge.entity.Loan;
import com.lendbridge.repository.EmiScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.itextpdf.text.pdf.draw.LineSeparator;

@Service
@RequiredArgsConstructor
public class LoanAgreementService {

    private final LoanService loanService;
    private final EmiScheduleRepository emiRepo;

    public byte[] generateAgreement(Long loanId) throws Exception {
        Loan loan = loanService.findLoanById(loanId);
        List<EmiSchedule> schedule = emiRepo.findByLoanIdOrderByEmiNumber(loanId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Colors
        BaseColor gold = new BaseColor(212, 175, 55);
        BaseColor dark = new BaseColor(15, 15, 20);
        BaseColor gray = new BaseColor(100, 100, 110);

        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, gold);
        Font headingFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, dark);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, dark);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, gray);
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font tableFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, dark);

        // Header
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 2});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingBottom(10);
        Paragraph logo = new Paragraph("LendBridge", titleFont);
        logoCell.addElement(logo);
        Paragraph tagline = new Paragraph("Peer-to-Peer Lending Platform", smallFont);
        logoCell.addElement(tagline);
        header.addCell(logoCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.setPaddingBottom(10);
        Paragraph agreementNo = new Paragraph("LOAN AGREEMENT", headingFont);
        agreementNo.setAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(agreementNo);
        Paragraph loanNo = new Paragraph("Agreement No: LB-" + loanId + "-2025", smallFont);
        loanNo.setAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(loanNo);
        Paragraph date = new Paragraph("Date: " + loan.getDisbursedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), smallFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(date);
        header.addCell(infoCell);

        doc.add(header);

        // Divider
        LineSeparator line = new LineSeparator(1, 100, gold, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);

        // Parties Section
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setSpacingBefore(10);
        parties.setSpacingAfter(10);

        // Borrower
        PdfPCell borrowerCell = new PdfPCell();
        borrowerCell.setBorder(Rectangle.BOX);
        borrowerCell.setBorderColor(new BaseColor(230, 230, 230));
        borrowerCell.setPadding(12);
        borrowerCell.addElement(new Paragraph("BORROWER", new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, gold)));
        borrowerCell.addElement(new Paragraph(
            loan.getBorrower().getFirstName() + " " + loan.getBorrower().getLastName(), headingFont));
        borrowerCell.addElement(new Paragraph("ID: " + loan.getBorrower().getId(), smallFont));
        borrowerCell.addElement(new Paragraph("Phone: " + loan.getBorrower().getPhoneNumber(), smallFont));
        parties.addCell(borrowerCell);

        // Lender
        PdfPCell lenderCell = new PdfPCell();
        lenderCell.setBorder(Rectangle.BOX);
        lenderCell.setBorderColor(new BaseColor(230, 230, 230));
        lenderCell.setPadding(12);
        lenderCell.addElement(new Paragraph("LENDER", new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, gold)));
        lenderCell.addElement(new Paragraph(
            loan.getLender().getFirstName() + " " + loan.getLender().getLastName(), headingFont));
        lenderCell.addElement(new Paragraph("ID: " + loan.getLender().getId(), smallFont));
        lenderCell.addElement(new Paragraph("Phone: " + loan.getLender().getPhoneNumber(), smallFont));
        parties.addCell(lenderCell);

        doc.add(parties);

        // Loan Details
        Paragraph detailsTitle = new Paragraph("LOAN DETAILS", headingFont);
        detailsTitle.setSpacingBefore(10);
        detailsTitle.setSpacingAfter(8);
        doc.add(detailsTitle);

        PdfPTable details = new PdfPTable(4);
        details.setWidthPercentage(100);
        details.setSpacingAfter(15);

        String[][] detailData = {
            {"Principal Amount", "₹" + loan.getPrincipalAmount().toPlainString(),
             "Interest Rate", loan.getAnnualInterestRate() + "% p.a."},
            {"Tenure", loan.getTenureMonths() + " months",
             "EMI Amount", "₹" + loan.getEmiAmount().toPlainString()},
            {"Disbursement Date", loan.getDisbursedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
             "First EMI Date", loan.getNextEmiDate().minusMonths(loan.getTenureMonths()-1).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))},
        };

        for (String[] row : detailData) {
            for (int i = 0; i < row.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(row[i], i % 2 == 0 ? smallFont : normalFont));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setBackgroundColor(i % 2 == 0 ? new BaseColor(248, 248, 248) : BaseColor.WHITE);
                cell.setPadding(6);
                details.addCell(cell);
            }
        }
        doc.add(details);

        // EMI Schedule Table
        Paragraph scheduleTitle = new Paragraph("EMI REPAYMENT SCHEDULE", headingFont);
        scheduleTitle.setSpacingBefore(5);
        scheduleTitle.setSpacingAfter(8);
        doc.add(scheduleTitle);

        PdfPTable emiTable = new PdfPTable(6);
        emiTable.setWidthPercentage(100);
        emiTable.setWidths(new float[]{0.5f, 1.2f, 1f, 1f, 1f, 1f});

        String[] headers = {"#", "Due Date", "EMI", "Principal", "Interest", "Balance"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, tableHeaderFont));
            hCell.setBackgroundColor(dark);
            hCell.setPadding(6);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emiTable.addCell(hCell);
        }

        for (EmiSchedule emi : schedule) {
            BaseColor rowBg = emi.getEmiNumber() % 2 == 0 ? new BaseColor(248, 248, 252) : BaseColor.WHITE;
            String[] row = {
                String.valueOf(emi.getEmiNumber()),
                emi.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                "₹" + emi.getEmiAmount().toPlainString(),
                "₹" + emi.getPrincipalComponent().toPlainString(),
                "₹" + emi.getInterestComponent().toPlainString(),
                "₹" + emi.getClosingBalance().toPlainString()
            };
            for (String val : row) {
                PdfPCell cell = new PdfPCell(new Phrase(val, tableFont));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emiTable.addCell(cell);
            }
        }
        doc.add(emiTable);

        // Terms
        doc.add(Chunk.NEWLINE);
        Paragraph terms = new Paragraph("TERMS & CONDITIONS", headingFont);
        terms.setSpacingBefore(10);
        terms.setSpacingAfter(6);
        doc.add(terms);

        String[] termsList = {
            "1. The borrower agrees to repay the loan amount along with interest as per the EMI schedule.",
            "2. Late payment will attract a penalty of 2% per month on overdue amount.",
            "3. Early closure is permitted with a 2% penalty on outstanding principal.",
            "4. This agreement is governed by the laws of India.",
            "5. LendBridge acts as a facilitator and is not responsible for default."
        };
        for (String term : termsList) {
            doc.add(new Paragraph(term, smallFont));
        }

        // Signatures
        doc.add(Chunk.NEWLINE);
        PdfPTable sigTable = new PdfPTable(3);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(20);

        String[] sigLabels = {"Borrower Signature", "Lender Signature", "LendBridge Authority"};
        for (String label : sigLabels) {
            PdfPCell sigCell = new PdfPCell();
            sigCell.setBorder(Rectangle.TOP);
            sigCell.setBorderColorTop(dark);
            sigCell.setPaddingTop(8);
            sigCell.addElement(new Paragraph(label, smallFont));
            sigTable.addCell(sigCell);
        }
        doc.add(sigTable);

        // Footer
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
            "This is a digitally generated loan agreement by LendBridge Platform. | lendbridge-backend.onrender.com",
            new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, gray));
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }
}