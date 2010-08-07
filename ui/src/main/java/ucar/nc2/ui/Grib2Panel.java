/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import ucar.grib.GribGridRecord;
import ucar.grib.GribNumbers;
import ucar.grib.GribPds;
import ucar.grib.grib1.Grib1Dump;
import ucar.grib.grib1.Grib1Pds;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib1.GribPDSLevel;
import ucar.grid.GridIndex;
import ucar.grid.GridRecord;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.grib.GribGridServiceProvider;
import ucar.nc2.iosp.grib.tables.GribTemplate;
import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.iosp.grid.GridVariable;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.grib.grib2.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ToolsUI/Iosp/Grib2
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class Grib2Panel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted recordTable, gdsTable, productTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;

  private Map<Integer, GribTemplate> productTemplates = null;

  private NetcdfFile ncd;

  public Grib2Panel(PreferencesExt prefs) {
    this.prefs = prefs;

    thredds.ui.PopupMenu varPopup;

    productTable = new BeanTableSorted(Product.class, (PreferencesExt) prefs.node("Product"), false, "GribGridRecord group", "same param / levelType");
    productTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Product pb = (Product) productTable.getSelectedBean();
        if (pb != null) {
          recordTable.setBeans(pb.list);
          // System.out.printf("records = %d%n", pb.getRecordBeans().size());
        }
      }
    });
    varPopup = new thredds.ui.PopupMenu(productTable.getJTable(), "Options");
    /* varPopup.addAction("Run accum algorithm", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Product pb = (Product) productTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          pb.doAccumAlgo(f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    }); */

    varPopup.addAction("Show record -> variable data assignments", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Product pb = (Product) productTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          showRecords(pb, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    recordTable = new BeanTableSorted(GribGridRecordBean.class, (PreferencesExt) prefs.node("IndexRecordBean"), false, "GribGridRecord", "one record");

    varPopup = new thredds.ui.PopupMenu(recordTable.getJTable(), "Options");
    varPopup.addAction("Show GridRecords", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = recordTable.getSelectedBeans();
        infoPopup2.clear();
        for (int i = 0; i < list.size(); i++) {
          GribGridRecordBean bean = (GribGridRecordBean) list.get(i);
          infoPopup2.appendLine(bean.ggr.toString());
          infoPopup2.appendLine("");
        }
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    varPopup.addAction("Compare GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = recordTable.getSelectedBeans();
        if (list.size() == 2) {
          GribGridRecordBean bean1 = (GribGridRecordBean) list.get(0);
          GribGridRecordBean bean2 = (GribGridRecordBean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show record -> variable data assignments", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = recordTable.getSelectedBeans();
        for (int i = 0; i < list.size(); i++) {
          GribGridRecordBean bean = (GribGridRecordBean) list.get(i);
          showRecord(bean.ggr, f);
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    varPopup.addAction("Show raw PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = recordTable.getSelectedBeans();
        for (int i = 0; i < list.size(); i++) {
          GribGridRecordBean bean = (GribGridRecordBean) list.get(i);
          showRawPds(bean.ggr, f);
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    gdsTable = new BeanTableSorted(GdsBean.class, (PreferencesExt) prefs.node("GdsBean"), false, "Grib2GridDefinitionSection", "unique from Grib2Records");
    gdsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GdsBean bean = (GdsBean) gdsTable.getSelectedBean();
        infoPopup.setText(bean.gds.toString());
        infoWindow.setVisible(true);
        gdsTable.clearSelection();
      }
    });

    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    infoPopup3 = new TextHistoryPane();
    infoWindow3 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup3);
    infoWindow3.setBounds((Rectangle) prefs.getBean("InfoWindowBounds3", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, productTable, recordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    recordTable.saveState(false);
    gdsTable.saveState(false);
    productTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  // new way - uses the index

  public void setGribFile(String filename) throws IOException {
    if (ncd != null) ncd.close();

    GridServiceProvider.debugOpen = true;
    ncd = NetcdfFile.open(filename);

    GribGridServiceProvider iosp = (GribGridServiceProvider) ncd.getIosp();
    GridIndex index = (GridIndex) iosp.sendIospMessage("GridIndex");

    Map<Integer, Product> pdsSet = new HashMap<Integer, Product>();

    java.util.List<Product> products = new ArrayList<Product>();
    List<GridRecord> grList = index.getGridRecords();
    for (GridRecord gr : grList) {
      GribGridRecord ggr = (GribGridRecord) gr;

      Product bean = pdsSet.get(ggr.cdmVariableHash());
      if (bean == null) {
        bean = new Product(ggr);
        pdsSet.put(ggr.cdmVariableHash(), bean);
        products.add(bean);
      }
      bean.list.add(new GribGridRecordBean(ggr));
    }

    List<Product> sortList = new ArrayList<Product>();
    sortList.addAll(pdsSet.values());
    Collections.sort(sortList);
    for (Product p : sortList)
      p.sort();

    GridServiceProvider.debugOpen = false;

    productTable.setBeans(products);
    recordTable.setBeans(new ArrayList());
  }

  public void showInfo(Formatter f) {
    if (ncd == null) return;

    int missing = 0;
    for (Variable v : ncd.getVariables()) {
      GridVariable pv = (GridVariable) v.getSPobject();
      if (pv == null) {
        f.format("%s has no GridVariable%n", v.getShortName());
      } else {
        missing += pv.showMissingSummary(f);
      }
    }
    f.format("Toal Missing = %d%n", missing);
  }

  private void showRecords(Product bean, Formatter f) {
    for (GribGridRecordBean ibean : bean.list) {
      showRecord(ibean.ggr, f);
    }
  }

  private void showRecord(GribGridRecord ggr, Formatter f) {
    GridVariable.Belongs b = (GridVariable.Belongs) ggr.getBelongs();
    f.format("%s == %s : ", ggr.toString2(), ggr.getBelongs());
    b.gv.showRecord(b.recnum, f);
    f.format("%n");
  }

  /*
      public int edition; // grib 1 or 2

  public int discipline;  // grib 2 only  ?
  public Date refTime;
  public int center = -1, subCenter = -1, table = -1;
  public int productTemplate, category, paramNumber;
  public int typeGenProcess;
  public int analGenProcess;
  public int levelType1, levelType2;
  public double levelValue1, levelValue2;
  public int forecastTime;
  public int startOfInterval = GribNumbers.UNDEFINED;
  public int timeUnit;
  private Date validTime = null;
  public int decimalScale = GribNumbers.UNDEFINED;
  public boolean isEnsemble = false;
  public int ensembleNumber  = GribNumbers.UNDEFINED;
  public int numberForecasts = GribNumbers.UNDEFINED;
  public int type = GribNumbers.UNDEFINED;
  public float lowerLimit = GribNumbers.UNDEFINED, upperLimit = GribNumbers.UNDEFINED;
  public int intervalStatType;
  */

  void compare(GribGridRecordBean bean1, GribGridRecordBean bean2, Formatter f) {
    GribGridRecord ggr1 = bean1.ggr;
    GribGridRecord ggr2 = bean2.ggr;

    /*


    boolean ok = true;
    if (ggr1.discipline != ggr2.discipline) {
      f.format("discipline differs %d != %d %n", ggr1.discipline, ggr2.discipline);
      ok = false;
    }
    if (ggr1.edition != ggr2.edition) {
      f.format("edition differs %d != %d %n", ggr1.edition, ggr2.edition);
      ok = false;
    }
    if (ggr1.center != ggr2.center) {
      f.format("center differs %d != %d %n", ggr1.center, ggr2.center);
      ok = false;
    }
    if (ggr1.subCenter != ggr2.subCenter) {
      f.format("subCenter differs %d != %d %n", ggr1.subCenter, ggr2.subCenter);
      ok = false;
    }
    if (ggr1.table != ggr2.table) {
      f.format("table differs %d != %d %n", ggr1.table, ggr2.table);
      ok = false;
    }
    if (ggr1.productTemplate != ggr2.productTemplate) {
      f.format("productTemplate differs %d != %d %n", ggr1.productTemplate, ggr2.productTemplate);
      ok = false;
    }
    if (ggr1.category != ggr2.category) {
      f.format("category differs %d != %d %n", ggr1.category, ggr2.category);
      ok = false;
    }
    if (ggr1.paramNumber != ggr2.paramNumber) {
      f.format("paramNumber differs %d != %d %n", ggr1.paramNumber, ggr2.paramNumber);
      ok = false;
    }
    if (ggr1.typeGenProcess != ggr2.typeGenProcess) {
      f.format("typeGenProcess differs %d != %d %n", ggr1.typeGenProcess, ggr2.typeGenProcess);
      ok = false;
    }
    if (ggr1.analGenProcess != ggr2.analGenProcess) {
      f.format("analGenProcess differs %d != %d %n", ggr1.analGenProcess, ggr2.analGenProcess);
      ok = false;
    }
    if (ggr1.levelValue1 != ggr2.levelValue1) {
      f.format("levelValue1 differs %f != %f %n", ggr1.levelValue1, ggr2.levelValue1);
      ok = false;
    }
    if (ggr1.levelValue2 != ggr2.levelValue2) {
      f.format("levelValue2 differs %f != %f %n", ggr1.levelValue2, ggr2.levelValue2);
      ok = false;
    }
    if (ggr1.forecastTime != ggr2.forecastTime) {
      f.format("forecastTime differs %d != %d %n", ggr1.forecastTime, ggr2.forecastTime);
      ok = false;
    }
    if (ggr1.startOfInterval != ggr2.startOfInterval) {
      f.format("startOfInterval differs %d != %d %n", ggr1.startOfInterval, ggr2.startOfInterval);
      ok = false;
    }
    if (ggr1.timeUnit != ggr2.timeUnit) {
      f.format("timeUnit differs %d != %d %n", ggr1.timeUnit, ggr2.timeUnit);
      ok = false;
    }
    if (ggr1.decimalScale != ggr2.decimalScale) {
      f.format("decimalScale differs %d != %d %n", ggr1.decimalScale, ggr2.decimalScale);
      ok = false;
    }
    if (ggr1.ensembleNumber != ggr2.ensembleNumber) {
      f.format("ensembleNumber differs %d != %d %n", ggr1.ensembleNumber, ggr2.ensembleNumber);
      ok = false;
    }
    if (ggr1.intervalStatType != ggr2.intervalStatType) {
      f.format("intervalStatType differs %d != %d %n", ggr1.intervalStatType, ggr2.intervalStatType);
      ok = false;
    }
    if (ggr1.lowerLimit != ggr2.lowerLimit) {
      f.format("lowerLimit differs %f != %f %n", ggr1.lowerLimit, ggr2.lowerLimit);
      ok = false;
    }
    if (ggr1.upperLimit != ggr2.upperLimit) {
      f.format("upperLimit differs %f != %f %n", ggr1.upperLimit, ggr2.upperLimit);
      ok = false;
    }
    if ((ggr1.refTime != null) && !ggr1.refTime.equals(ggr2.refTime)) {
      f.format("refTime differs %s != %s %n", ggr1.refTime, ggr2.refTime);
      ok = false;
    }
    if ((ggr1.validTime != null) && !ggr1.validTime.equals(ggr2.validTime)) {
      f.format("refTime differs %s != %s %n", ggr1.validTime, ggr2.validTime);
      ok = false;
    }

    if (ok) f.format("All OK!%n");
    return f.toString();  */

    f.format("%n");
    byte[] raw1 = ggr1.getPds().getPDSBytes();
    byte[] raw2 = ggr2.getPds().getPDSBytes();
    if (raw1.length != raw2.length) {
      f.format("length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i])
        f.format(" %3d : %3d != %3d%n", i + 1, raw1[i], raw2[i]);
    }
    return;
  }

  public class Product implements Comparable<Product> {
    GribGridRecord ggr;
    List<GribGridRecordBean> list = new ArrayList<GribGridRecordBean>();
    String name;

    Product() {
    }

    Product(GribGridRecord ggr) {
      this.ggr = ggr;
      this.name = ggr.getParameterDescription();
    }

    ///////////////

    public String getName() {
      return name;
    }

    public String getParamNo() {
      if (ggr.getEdition() == 1)
        return ggr.getParameterNumber()+" ";
      else {
        Grib2Pds pds2 = (Grib2Pds) ggr.getPds();
        return ggr.getDiscipline() + "-" + pds2.getParameterCategory() + "-" + ggr.getParameterNumber();
      }
    }

    public int getLevelType() {
      return ggr.getLevelType1();
    }

    public String getSuffix() {
      return ggr.makeSuffix();
    }

    /* public final String getCenter() {
      return Grib1Tables.getCenter_idName(ggr.center) + " (" + ggr.center + "/" + ggr.subCenter + ")";
    }

    public final int getTable() {
      return ggr.table;
    } */

    public int getTemplate() {
      if (ggr.getEdition() == 1)
        return -1;
      else {
        Grib2Pds pds2 = (Grib2Pds) ggr.getPds();
        return pds2.getProductDefinitionTemplate();
      }
    }

    public final Date getRefTime() {
      return ggr.getReferenceTime();
    }

    public final String getLevelName() {   // LOOK this kind of stuff needs to be pushed down into GRIB library !
      if (ggr.getEdition() == 2)
        return Grib2Tables.codeTable4_5(ggr.getLevelType1());
      else
        return Grib1Tables.getLevelDescription(ggr.getLevelType1());
    }

    public final String getGenProc() {
      if (ggr.getEdition() == 2)
        return Grib2Tables.codeTable4_3(ggr.getTypeGenProcess());
      else
        return Grib1Tables.getTypeGenProcessName(ggr.getCenter(), ggr.getTypeGenProcess());
    }

    public final String getStatType() {
      if (ggr.getPds().isInterval())
        return Grib2Tables.codeTable4_10short(ggr.getPds().getIntervalStatType());
      else
        return "";
    }

    public int getN() {
      return list.size();
    }

    public final int getNEns() {
      if (isEnsemble() || isDerived()) {
        return ggr.getPds().getNumberEnsembleForecasts();
      }
      return -1;
    }

    public final boolean isEnsemble() {
      return ggr.getPds().isEnsemble();
    }

    public final boolean isDerived() {
      return ggr.getPds().isEnsembleDerived();
    }

    void sort() {
      Collections.sort(list, new Comparator<GribGridRecordBean>() {

        @Override
        public int compare(GribGridRecordBean o1, GribGridRecordBean o2) {
          if (getNEns() >= 0) {
            if (o1.getForecastTime() == o2.getForecastTime())
              return o1.getPertNumber() - o2.getPertNumber();
          }
          return o1.getForecastTime() - o2.getForecastTime();
        }
      });
    }

    /* private void doAccumAlgo(Formatter f) {
      if (getNEns() < 0) {
        doAccumAlgo(list, f);
        return;
      } else {
        HashMap<Integer, List<GribGridRecordBean>> map = new HashMap<Integer, List<GribGridRecordBean>>();
        for (GribGridRecordBean bean : list) {
          List<GribGridRecordBean> list = map.get(bean.getEnsNumber());
          if (list == null) {
            list = new ArrayList<GribGridRecordBean>();
            map.put(bean.getEnsNumber(), list);
          }
          list.add(bean);
        }
        for (int ensNo : map.keySet()) {
          List<GribGridRecordBean> list = map.get(ensNo);
          f.format("==========Ensemble %d %n", ensNo);
          doAccumAlgo(list, f);
        }
        return;
      }
    }

    private void doAccumAlgo(List<GribGridRecordBean> list, Formatter f) {
      List<GribGridRecord> all = new ArrayList<GribGridRecord>();
      List<GribGridRecord> hourAccum = new ArrayList<GribGridRecord>(all.size());
      List<GribGridRecord> runAccum = new ArrayList<GribGridRecord>(all.size());

      Set<Integer> ftimes = new HashSet<Integer>();

      for (GribGridRecordBean bean : list) {
        GribGridRecord rb = bean.ggr;
        all.add(rb);

        int ftime = rb.forecastTime;
        ftimes.add(ftime);

        int start = rb.startOfInterval;
        int end = rb.forecastTime;
        if (end - start == 1) hourAccum.add(rb);
        if (start == 0) runAccum.add(rb);
      }

      int n = ftimes.size();

      f.format("      all: ");
      showList(all, f);

      if (hourAccum.size() > n - 2) {
        for (GribGridRecord rb : hourAccum) all.remove(rb);
        f.format("hourAccum: ");
        showList(hourAccum, f);
      }

      if (runAccum.size() > n - 2) {
        for (GribGridRecord rb : runAccum) all.remove(rb);
        f.format(" runAccum: ");
        showList(runAccum, f);
      }

      if ((all.size() > 0) && (all.size() != list.size())) {
        f.format("remaining: ");
        showList(all, f);
      }

    }

    private String testConstantInterval(List<GribGridRecord> list) {
      boolean same = true;
      int intv = -1;
      for (GribGridRecord rb : list) {
        int start = rb.startOfInterval;
        int end = rb.forecastTime;
        int intv2 = end - start;
        if (intv2 == 0) continue; // skip those weird zero-intervals
        else if (intv < 0) intv = intv2;
        else same = (intv == intv2);
        if (!same) break;
      }
      return same ? " Interval=" + intv : " Mixed";
    }

    private void showList(List<GribGridRecord> list, Formatter f) {
      f.format("(%d) ", list.size());
      for (GribGridRecord rb : list)
        f.format(" %d-%d", rb.startOfInterval, rb.forecastTime);
      f.format(" %s %n", testConstantInterval(list));
    } */


    @Override
    public int compareTo(Product o) {
      return name.compareTo(o.name);
    }
  }

  public class GribGridRecordBean {
    GribGridRecord ggr;
    GribPds pds;;

    // no-arg constructor

    public GribGridRecordBean() {
    }

    public GribGridRecordBean(GribGridRecord ggr) {
      this.ggr = ggr;
      this.pds = ggr.getPds();
    }

    public final String getTimeUnit() {
      return ggr.getTimeUnitName();
    }

    public final String getSurfaceType() {
      if (ggr.getEdition() == 2) {
        String s = Grib2Tables.getTypeSurfaceNameShort(ggr.getLevelType1());
        int lev2 = ggr.getLevelType2();
        if ((lev2 != GribNumbers.UNDEFINED) && (lev2 != GribNumbers.MISSING))
          s += "/" + Grib2Tables.getTypeSurfaceNameShort(lev2);
        return s;

      } else {
        String s = GribPDSLevel.getLevelDescription(ggr.getLevelType1());
        int lev2 = ggr.getLevelType2();
        if ((lev2 != GribNumbers.UNDEFINED) && (lev2 != GribNumbers.MISSING))
          s += "/" + GribPDSLevel.getLevelDescription(lev2);
        return s;
      }
    }

    public String getSurfaceValue() {
        return ggr.getLevel1() + "-" + ggr.getLevel2();
    }

    public String getInterval() {
      if (pds.isInterval()) {
        int[] intv = pds.getForecastTimeInterval();
        return intv[0]+"-"+intv[1];
      }
      return "";
    }

    public Date getIntervalEnd() {
      if (pds.isInterval()) {
        return new Date(pds.getIntervalTimeEnd());
      }
      return null;
    }

    public Date getValidTime() {
      return pds.getForecastDate();
    }


    public int getForecastTime() {
      return pds.getForecastTime();
    }

    public final int getPertNumber() {
      if (pds.isEnsemble()) {
        return pds.getPerturbationNumber();
      }
      return -1;
    }

    public int getPertType() {
      if (pds.isEnsemble()) {
        return pds.getPerturbationType();
      }
      return -1;
    }

    public final String getProbability() {
      if (pds.isProbability()) {
        return pds.getProbabilityLowerLimit()+"-"+pds.getProbabilityUpperLimit();
      }
      return "";
    }

    public final int getLocalGenProc() {
      return pds.getTypeGenProcess();
    }

  }

   private void showRawPds(GribGridRecord ggr, Formatter f) {
    if (ggr.getEdition() == 1) {
      Grib1Dump.printPDS( (Grib1Pds) ggr.getPds(), f);
      byte[] raw = ggr.getPds().getPDSBytes();
      f.format("%n");
      for (int i= 28; i<raw.length; i++) {
        f.format(" %3d : %3d%n", i+1, raw[i]);
      }
      return;
    }

    Grib2Pds pds2 = (Grib2Pds) ggr.getPds();
    int template = pds2.getProductDefinitionTemplate();

    byte[] raw = ggr.getPds().getPDSBytes();
    if (raw == null) {
      f.format("PDS bytes not available template=%d%n", template);
      return;
    }
    showRawPds(template, raw, f);
  }

  private void showRawPds(int template, byte[] raw, Formatter f) {
    if (productTemplates == null)
      try {
        productTemplates = GribTemplate.getParameterTemplates();
      } catch (IOException e) {
        f.format("Read template failed = %s%n", e.getMessage());
        return;
      }

    GribTemplate gt = productTemplates.get(template);
    if (gt == null)
      f.format("Cant find template %d%n", template);
    else
      gt.showInfo(raw, f);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class GdsBean {
    Grib2GridDefinitionSection gds;
    int key;

    // no-arg constructor

    public GdsBean() {
    }

    public GdsBean(int key, Grib2GridDefinitionSection m) {
      this.key = key;
      this.gds = m;
    }

    public int getKey() {
      return key;
    }

    public int getHashCode() {
      return gds.hashCode();
    }

    public int getGridNo() {
      return gds.getGdtn();
    }

    public String getGridName() {
      return gds.getGridName(gds.getGdtn());
    }

    public String getScanMode() {
      return Long.toBinaryString(gds.getScanMode());
    }

    public String getResolution() {
      return Long.toBinaryString(gds.getResolution());
    }

  }


}