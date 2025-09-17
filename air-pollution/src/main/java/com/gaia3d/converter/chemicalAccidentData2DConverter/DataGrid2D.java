package com.gaia3d.converter.chemicalAccidentData2DConverter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class DataGrid2D {
    // This class contains the data of one grid (one pixel)
    private String gridId;
    private String number;
    private String accident_no;
    private String analysis_time;
    private String concentration;
    private String aegl1_eval;
    private String aegl2_eval;
    private String aegl3_eval;
    private String victim_count;
    private String acu_assment_cd;

    public DataGrid2D() {
        this.gridId = "";
        this.number = "";
        this.accident_no = "";
        this.analysis_time = "";
        this.concentration = "";
        this.aegl1_eval = "";
        this.aegl2_eval = "";
        this.aegl3_eval = "";
        this.victim_count = "";
        this.acu_assment_cd = "";
    }
}
