package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsArt26Curve;

public interface VqmsArt26CurveMapper {

    public VqmsArt26Curve selectVqmsArt26CurveById(Long curveId);

    /** 按母线 + 季度查考核曲线行。 */
    public List<VqmsArt26Curve> selectByBusbarQuarter(VqmsArt26Curve q);

    public int insertVqmsArt26Curve(VqmsArt26Curve vqmsArt26Curve);

    public int updateVqmsArt26Curve(VqmsArt26Curve vqmsArt26Curve);

    public int deleteVqmsArt26CurveByIds(Long[] curveIds);
}
