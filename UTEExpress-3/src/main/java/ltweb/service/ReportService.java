package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WarehouseReportRepository warehouseReportRepository;
    private final WarehouseRepository warehouseRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final OutboundReceiptRepository outboundReceiptRepository;
    private final InboundReceiptDetailRepository inboundReceiptDetailRepository;
    private final OutboundReceiptDetailRepository outboundReceiptDetailRepository;
    private final InventoryRepository inventoryRepository;

    public List<WarehouseReport> getAllReports() {
        return warehouseReportRepository.findAll();
    }

    public WarehouseReport getReportById(Long id) {
        return warehouseReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
    }

    public List<WarehouseReport> getReportsByWarehouseId(Long warehouseId) {
        return warehouseReportRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    public List<WarehouseReport> getReportsByWarehouseAndType(Long warehouseId, ReportType reportType) {
        return warehouseReportRepository.findByWarehouseIdAndReportType(warehouseId, reportType);
    }

    public List<WarehouseReport> getReportsByDateRange(Long warehouseId, LocalDateTime startDate, LocalDateTime endDate) {
        return warehouseReportRepository.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);
    }

    @Transactional
    public WarehouseReport generateReport(Long warehouseId, ReportType reportType, 
                                         LocalDateTime startDate, LocalDateTime endDate, 
                                         User createdBy, String notes) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Integer openingStock = calculateOpeningStock(warehouseId, startDate);
        
        List<InboundReceipt> inboundReceipts = inboundReceiptRepository
                .findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);
        
        List<OutboundReceipt> outboundReceipts = outboundReceiptRepository
                .findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);
        
        Integer totalInboundQuantity = calculateTotalInboundQuantity(inboundReceipts);
        Integer totalOutboundQuantity = calculateTotalOutboundQuantity(outboundReceipts);
        
        Integer closingStock = openingStock + totalInboundQuantity - totalOutboundQuantity;
        
        Double utilizationRate = warehouse.getTotalCapacity() != null && warehouse.getTotalCapacity() > 0
                ? (closingStock.doubleValue() / warehouse.getTotalCapacity()) * 100
                : 0.0;

        WarehouseReport report = WarehouseReport.builder()
                .warehouse(warehouse)
                .reportType(reportType)
                .startDate(startDate)
                .endDate(endDate)
                .totalInboundReceipts(inboundReceipts.size())
                .totalOutboundReceipts(outboundReceipts.size())
                .totalInboundQuantity(totalInboundQuantity)
                .totalOutboundQuantity(totalOutboundQuantity)
                .openingStock(openingStock)
                .closingStock(closingStock)
                .utilizationRate(utilizationRate)
                .createdBy(createdBy)
                .notes(notes)
                .build();

        return warehouseReportRepository.save(report);
    }

    @Transactional
    public WarehouseReport generateDailyReport(Long warehouseId, LocalDateTime date, User createdBy) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
        
        return generateReport(warehouseId, ReportType.DAILY, startOfDay, endOfDay, 
                createdBy, "Daily report for " + date.toLocalDate());
    }

    @Transactional
    public WarehouseReport generateWeeklyReport(Long warehouseId, LocalDateTime weekStart, User createdBy) {
        LocalDateTime startOfWeek = weekStart.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        
        return generateReport(warehouseId, ReportType.WEEKLY, startOfWeek, endOfWeek, 
                createdBy, "Weekly report");
    }

    @Transactional
    public WarehouseReport generateMonthlyReport(Long warehouseId, LocalDateTime month, User createdBy) {
        LocalDateTime startOfMonth = month.with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = month.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        return generateReport(warehouseId, ReportType.MONTHLY, startOfMonth, endOfMonth, 
                createdBy, "Monthly report for " + month.getMonth() + " " + month.getYear());
    }

    @Transactional
    public WarehouseReport generateQuarterlyReport(Long warehouseId, LocalDateTime quarter, User createdBy) {
        int quarterNumber = (quarter.getMonthValue() - 1) / 3 + 1;
        LocalDateTime startOfQuarter = quarter.withMonth((quarterNumber - 1) * 3 + 1)
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfQuarter = startOfQuarter.plusMonths(2)
                .with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23).withMinute(59).withSecond(59);
        
        return generateReport(warehouseId, ReportType.QUARTERLY, startOfQuarter, endOfQuarter, 
                createdBy, "Quarterly report Q" + quarterNumber + " " + quarter.getYear());
    }

    @Transactional
    public WarehouseReport generateYearlyReport(Long warehouseId, LocalDateTime year, User createdBy) {
        LocalDateTime startOfYear = year.with(TemporalAdjusters.firstDayOfYear())
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfYear = year.with(TemporalAdjusters.lastDayOfYear())
                .withHour(23).withMinute(59).withSecond(59);
        
        return generateReport(warehouseId, ReportType.YEARLY, startOfYear, endOfYear, 
                createdBy, "Yearly report for " + year.getYear());
    }

    private Integer calculateOpeningStock(Long warehouseId, LocalDateTime startDate) {
        Integer totalStock = inventoryRepository.getTotalRemainingQuantityByWarehouseId(warehouseId);
        
        List<InboundReceipt> inboundAfterStart = inboundReceiptRepository
                .findByWarehouseIdAndReceivedDateAfter(warehouseId, startDate);
        List<OutboundReceipt> outboundAfterStart = outboundReceiptRepository
                .findByWarehouseIdAndIssuedDateAfter(warehouseId, startDate);
        
        Integer inboundAfter = calculateTotalInboundQuantity(inboundAfterStart);
        Integer outboundAfter = calculateTotalOutboundQuantity(outboundAfterStart);
        
        return (totalStock != null ? totalStock : 0) - inboundAfter + outboundAfter;
    }

    private Integer calculateTotalInboundQuantity(List<InboundReceipt> receipts) {
        int total = 0;
        for (InboundReceipt receipt : receipts) {
            List<InboundReceiptDetail> details = inboundReceiptDetailRepository
                    .findByInboundReceiptId(receipt.getId());
            for (InboundReceiptDetail detail : details) {
                total += detail.getQuantity();
            }
        }
        return total;
    }

    private Integer calculateTotalOutboundQuantity(List<OutboundReceipt> receipts) {
        int total = 0;
        for (OutboundReceipt receipt : receipts) {
            List<OutboundReceiptDetail> details = outboundReceiptDetailRepository
                    .findByOutboundReceiptId(receipt.getId());
            for (OutboundReceiptDetail detail : details) {
                total += detail.getQuantity();
            }
        }
        return total;
    }

    @Transactional
    public void deleteReport(Long id) {
        warehouseReportRepository.deleteById(id);
    }
}