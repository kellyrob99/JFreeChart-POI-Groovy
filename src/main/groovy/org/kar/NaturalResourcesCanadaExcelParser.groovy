package org.kar

import extract.excel.ExcelBuilder
import org.apache.poi.hssf.usermodel.*

/*
 * Work Created: 12-03-17 by krobinson
 */
class NaturalResourcesCanadaExcelParser
{
    public static final String SHEET1 = 'Sheet1'
    public static final List<String> HEADERS = ['Species', 'EMPTY', 'Year', 'NL', 'PE', 'NS', 'NB', 'QC', 'ON', 'MB', 'SK', 'AB',
            'BC', 'YT', 'NT *a', 'NU', 'CA']
    public static final Map SYMBOLS = [sheet: SHEET1, offset: 910, max: 8]
    public static final Map PINE = [sheet: SHEET1, offset: 6, max: 21, species: 'Pine']
    public static final Map SPRUCE = [sheet: SHEET1, offset: 29, max: 21, species: 'Spruce']
    public static final Map FIR = [sheet: SHEET1, offset: 61, max: 21, species: 'Fir']
    public static final Map DOUGLAS_FIR = [sheet: SHEET1, offset: 84, max: 21, species: 'Douglas-fir']
    public static final Map MISCELLANEOUS_SOFTWOODS = [sheet: SHEET1, offset: 116, max: 21, species: 'Miscellaneous softwoods']
    public static final Map MISCELLANEOUS_HARDWOODS = [sheet: SHEET1, offset: 139, max: 21, species: 'Miscellaneous hardwoods']
    public static final Map UNSPECIFIED = [sheet: SHEET1, offset: 171, max: 21, species: 'Unspecified']
    public static final Map TOTAL_PLANTING = [sheet: SHEET1, offset: 194, max: 21, species: 'Total planting']
    public static final int HEADER_OFFSET = 3
    public static final List<Map> PROVINCIAL = [PINE, SPRUCE, FIR, DOUGLAS_FIR, MISCELLANEOUS_SOFTWOODS, MISCELLANEOUS_HARDWOODS, UNSPECIFIED, TOTAL_PLANTING]
    public static final List<Map> PRIVATE_LAND = offset(PROVINCIAL, 220)
    public static final List<Map> FEDERAL = offset(PROVINCIAL, 441)
    public static final List<Map> TOTAL = offset(PROVINCIAL, 662)
    public static final List<String> SPECIES = PROVINCIAL.collect {it.species}
    public static final List<String> AREAS = HEADERS[HEADER_OFFSET..-1]
    public static final ArrayList<String> GROUPINGS = ['Provincial', 'Private Land', 'Federal', 'Total']
    public static final Map<String, String> PROVINCE_SHORT_FORM_MAPPINGS = ['Alberta': 'AB',
            'British Columbia': 'BC',
            'Manitoba': 'MB',
            'New Brunswick': 'NB',
            'Newfoundland and Labrador': 'NL',
            'Northwest Territories': 'NT *a',
            'Nova Scotia': 'NS',
            'Nunavut': 'NU',
            'Ontario': 'ON',
            'Prince Edward Island': 'PE',
            'Quebec': 'QC',
            'Saskatchewan': 'SK',
            'Yukon': 'YT',
            'Canada': 'CA']

    private static List<Map> offset(List<Map> maps, int offset)
    {
        maps.collect { Map map ->
            Map offsetMap = new LinkedHashMap(map)
            offsetMap.offset = offsetMap.offset + offset
            offsetMap
        }
    }

    public Map convertToMap(File data)
    {
        final ExcelBuilder builder = new ExcelBuilder(data.absolutePath)
        Map<String, String> symbolTable = [:]
        builder.eachLine(SYMBOLS) { HSSFRow row ->
            symbolTable[row.getCell(0).stringCellValue] = row.getCell(1).stringCellValue
        }

        final Closure collector = { Map species ->
            Map speciesMap = [name: species.species]
            builder.eachLine(species) {HSSFRow row ->
                //ensure that we are reading from the correct place in the file
                if (row.rowNum == species.offset)
                {
                    assert row.getCell(0).stringCellValue == species.species
                }
                //process rows
                if (row.rowNum > species.offset)
                {
                    final int year = row.getCell(HEADERS.indexOf('Year')).stringCellValue as int
                    Map yearMap = [:]
                    expandHeaders(AREAS).eachWithIndex {String header, int index ->
                        final HSSFCell cell = row.getCell(index + HEADER_OFFSET)
                        yearMap[header] = cell.cellType == HSSFCell.CELL_TYPE_STRING ? cell.stringCellValue : cell.numericCellValue
                    }
                    speciesMap[year] = yearMap.asImmutable()
                }
            }
            speciesMap.asImmutable()
        }
        def parsedSpreadsheet = [PROVINCIAL, PRIVATE_LAND, FEDERAL, TOTAL].collect {
            it.collect(collector)
        }
        Map resultsMap = [:]
        GROUPINGS.eachWithIndex {String groupName, int index ->
            resultsMap[groupName] = parsedSpreadsheet[index]
        }
        resultsMap['Symbols'] = symbolTable
        resultsMap
    }

    private List<String> expandHeaders(List<String> strings)
    {
        strings.collect {[it, "${it}_notes"]}.flatten()
    }
}
