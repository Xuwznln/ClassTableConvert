package com.wznln.mc.var;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.wznln.mc.method.Ttool.*;
import static com.wznln.mc.var.Oconfig.getConfig;
import static com.wznln.mc.var.Oconfig.loadConfig;

public class ClassTable{
    private int mode = 1;
    public static int XI = 1;
    public static int NK = 2;
    private String info;
    private final HashMap<Integer, List<Integer>> week_info = new HashMap<>();
    private final HashMap<Integer,String> class_name = new HashMap<>();
    private final HashMap<Integer,List<Integer>> begin_end = new HashMap<>();
    private final HashMap<Integer,String> class_where = new HashMap<>();
    private final HashMap<Integer,String> class_with_who = new HashMap<>();
    private final HashMap<Integer,String> class_addition_info = new HashMap<>();
    private final HashMap<Integer,Integer> class_turn = new HashMap<>();
    private final HashMap<Integer,Calendar> class_date = new HashMap<>();
    private final HashMap<Integer,Calendar> class_end_date = new HashMap<>();
    private final HashMap<Calendar,Calendar> change_class_date = new HashMap<Calendar,Calendar>(){{
        SimpleDateFormat t = new SimpleDateFormat("yyyy年MM月dd日");
        try {
            put(giveCalendar(t.parse("2021年5月3日")), giveCalendar(t.parse("2021年4月25日")));
            put(giveCalendar(t.parse("2021年5月4日")), giveCalendar(t.parse("2021年5月8日")));
            put(giveCalendar(t.parse("2021年5月8日")), giveCalendar(t.parse("2021年5月9日")));
        } catch (ParseException ex) {
            PrintError(ex,"转换日期出错");
        }
    }};
    private final HashMap<Integer,Calendar> class_changed = new HashMap<Integer,Calendar>(){{  }};
    private int last_avail_number = 1;
    private int week_max = 0;
    private int day_max = 0;
    private int end_max = 0;
    private boolean broadcast = false;
    private final Calendar start_day = Calendar.getInstance();
    SimpleDateFormat full = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
    SimpleDateFormat min = new SimpleDateFormat("yyyy年MM月dd日");
    SimpleDateFormat minmin = new SimpleDateFormat("MM月dd日");
    SimpleDateFormat hour = new SimpleDateFormat("HH:mm");

    public static void main(String[] args){
        Constants.RUNNING = true;
        Constants.NOT_MAIN =false;
        Constants.QQ_ENABLE =true;
        loadConfig();
        ClassTable yh2 = new ClassTable(
          "形势与政策\t没用的数字\t开设学院\t12-15周,8-9周\t星期三\t第3节\t第4节\t没用的数字\tN8606\t上课班级\n" +
            "科目2\t没用的数字\t开设学院\t10周\t星期五\t第1节\t第5节\t没用的数字\tA406\t上课班级\n");

    }

    public void setInfo(String info) {
        this.info = info;
    }
    public ClassTable(String info){
        this.info = info;
        init();
    }
    public ClassTable(String info,int mode){
        this.info = info;
        this.mode = mode;
        init();
    }

    private void init(){
        start_day.set(2021, Calendar.MARCH,1);
        start_day.setFirstDayOfWeek(Calendar.MONDAY);
        String[] each = info.split("\n");
        for(String info_s : each){
            String[] info = info_s.split("\t");
            List<Integer> weeks = each_class_weeks(info[3]);
            for(int temp = 0 ; temp<weeks.size();temp++){
                class_where.put(last_avail_number,info[8]);
                class_name.put(last_avail_number,info[0]);
                class_with_who.put(last_avail_number,info[9]);
//                class_addition_info.put(last_avail_number,info.length==11?info[10]:"试运行，请及时反馈");
                class_addition_info.put(last_avail_number,info.length==11?info[10]:"");
                begin_end.put(last_avail_number,get_begin_end(info[5],info[6]));
                int finalTemp = temp;
                week_info.put(last_avail_number, new ArrayList<Integer>() {{
                    add(weeks.get(finalTemp));
                    add(get_day(info[4]));
                }});
                Calendar temp_cale = Calendar.getInstance();
                temp_cale.set(2021, Calendar.MARCH,1);
                temp_cale.add(Calendar.WEEK_OF_YEAR,week_info.get(last_avail_number).get(0)-1);
                temp_cale.add(Calendar.DAY_OF_YEAR,week_info.get(last_avail_number).get(1)-1);
                List<Integer> time = getClassTimeHourAndMinuteStartAndEnd(begin_end.get(last_avail_number));
                temp_cale.set(Calendar.HOUR_OF_DAY,time.get(0));
                temp_cale.set(Calendar.MINUTE,time.get(1));
                Calendar got = getChangeCalendar(temp_cale);
                if(got!=temp_cale){
                    class_addition_info.put(last_avail_number,"调休，原始时间："+min.format(temp_cale.getTime()));
                    temp_cale.set(Calendar.DAY_OF_YEAR,got.get(Calendar.DAY_OF_YEAR)); //后续要改成年份也该
                    week_info.put(last_avail_number,new ArrayList<Integer>(){{
                        add(getWeek(temp_cale));
                        add(getDay(temp_cale));
                    }});
                }

                class_date.put(last_avail_number,temp_cale);
                Calendar temp_end_cale = Calendar.getInstance();
                temp_end_cale.set(2021, Calendar.MARCH,1);
                temp_end_cale.set(Calendar.DAY_OF_YEAR,temp_cale.get(Calendar.DAY_OF_YEAR));
                temp_end_cale.set(Calendar.HOUR_OF_DAY,time.get(2));
                temp_end_cale.set(Calendar.MINUTE,time.get(3));
                class_end_date.put(last_avail_number,temp_end_cale);
                last_avail_number++;
            }
        }
        sortAll();
    }
    public long func(String name, String qq){
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        List<Integer> today_class = get_today_class(calendar);
        if(name.startsWith("查周")&&name.length()>5){
            List<Integer> timed_class = new ArrayList<>();
            Calendar new_cale = (Calendar) calendar.clone();
            new_cale.set(Calendar.HOUR_OF_DAY,1);
            new_cale.set(Calendar.DAY_OF_WEEK,get_inner_day(name.substring(2,3)));
            try{
                String[] each = name.substring(3).split("-");
                int start = Integer.parseInt(each[0]);
                int end = Integer.parseInt(each[1]);
                for (Integer i : class_turn.values()){
                    if(class_end_date.get(i).get(Calendar.DAY_OF_WEEK)==new_cale.get(Calendar.DAY_OF_WEEK)&&class_end_date.get(i).after(new_cale)){
                        if(each.length==2){
                            for (int temp = start;temp<=end;temp++){
                                if(begin_end.get(i).contains(temp)){
                                    timed_class.add(i);
                                    break;
                                }
                            }
                        }else{
                            PrintQQ("格式错误", getConfig("QQPrint"), true, qq);
                            return 1000;
                        }
                    }
                }
                StringBuilder send_info = new StringBuilder();
                send_info.append("星期").append(get_day_str(getDay(new_cale))).append(" 第").append(start).append("到").append(end).append("节统计").append("\n");
                if(timed_class.size()==0) send_info.append("无课程安排");
                for (int temp=0;temp<timed_class.size();temp++) send_info.append(temp+1).append(". ").append(get_timed_class_info(timed_class.get(temp))).append("\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
            } catch (Exception e) {
                PrintQQ("格式错误", getConfig("QQPrint"), true, qq);
            }
            return 1000;
        }
        else if(name.startsWith("查")&&name.length()>1){
            List<Integer> timed_class = new ArrayList<>();
            try{
                Set<String> classes_name = new HashSet<>();
                String class_part_name = name.substring(1);
                for (Integer i : class_turn.values()){
                    if(class_name.get(i).contains(class_part_name)){
                        classes_name.add(class_name.get(i));
                        timed_class.add(i);
                    }
                }
                StringBuilder send_info = new StringBuilder();
                send_info.append("科目查询：").append("\n");
                if(timed_class.size()==0) send_info.append("未查到相关科目，请您更换搜索内容");
                for (int temp=0;temp<timed_class.size();temp++) send_info.append(temp+1).append(". ").append(get_subject_class_info(timed_class.get(temp),classes_name.size()==1)).append("\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
            } catch (Exception e) {
                PrintQQ("格式错误", getConfig("QQPrint"), true, qq);
            }
            return 1000;
        }
        switch (name){
            case "今日":
            case "今天":
            case "今":
                StringBuilder send_info = new StringBuilder();
                send_info.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    今日共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_info.append(temp+1).append(". ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "明日":
            case "明天":
            case "明":
                StringBuilder send_tomorrow = new StringBuilder();
                send_tomorrow.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.add(Calendar.DAY_OF_YEAR,1);
                today_class = get_today_class(calendar);
                send_tomorrow.append("明天上课第").append(getWeek(calendar)).append("周").append("    明日共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_tomorrow.append(temp+1).append(". ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_tomorrow.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "后日":
            case "后天":
            case "后":
                StringBuilder the_day_after_tomorrow = new StringBuilder();
                the_day_after_tomorrow.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.add(Calendar.DAY_OF_YEAR,2);
                today_class = get_today_class(calendar);
                the_day_after_tomorrow.append("后天上课第").append(getWeek(calendar)).append("周").append("    后天共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) the_day_after_tomorrow.append(temp+1).append(". ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(the_day_after_tomorrow.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "本周":
                StringBuilder send_this_week = new StringBuilder();
                send_this_week.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                today_class.clear();
                for (int temp=0;temp<7;temp++){
                    today_class.addAll(get_today_class(calendar));
                    calendar.add(Calendar.DAY_OF_YEAR,1);
                }
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,-1);
                send_this_week.append("上课第").append(getWeek(calendar)).append("周").append("    本周共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_this_week.append(temp+1).append(". 周").append(get_day_str(getDay(class_date.get(today_class.get(temp))))).append(" ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_this_week.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "下周":
                StringBuilder send_next_week = new StringBuilder();
                send_next_week.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,1);
                today_class.clear();
                for (int temp=0;temp<7;temp++){
                    today_class.addAll(get_today_class(calendar));
                    calendar.add(Calendar.DAY_OF_YEAR,1);
                }
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,-1);
                send_next_week.append("下周上课第").append(getWeek(calendar)).append("周").append("    下周共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_next_week.append(temp+1).append(". 周").append(get_day_str(getDay(class_date.get(today_class.get(temp))))).append(" ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_next_week.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "下下周":
                StringBuilder send_next_next_week = new StringBuilder();
                send_next_next_week.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,2);
                today_class.clear();
                for (int temp=0;temp<7;temp++){
                    today_class.addAll(get_today_class(calendar));
                    calendar.add(Calendar.DAY_OF_YEAR,1);
                }
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,-1);
                send_next_next_week.append("下下周上课第").append(getWeek(calendar)).append("周").append("    下下周共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_next_next_week.append(temp+1).append(". 周").append(get_day_str(getDay(class_date.get(today_class.get(temp))))).append(" ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_next_next_week.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "下下下周":
                StringBuilder send_next_next_next_week = new StringBuilder();
                send_next_next_next_week.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,3);
                today_class.clear();
                for (int temp=0;temp<7;temp++){
                    today_class.addAll(get_today_class(calendar));
                    calendar.add(Calendar.DAY_OF_YEAR,1);
                }
                calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
                calendar.add(Calendar.WEEK_OF_YEAR,-1);
                send_next_next_next_week.append("下下下周上课第").append(getWeek(calendar)).append("周").append("    下下下周共有").append(today_class.size()).append("门课程").append("\n====================\n");
                for (int temp=0;temp<today_class.size();temp++) send_next_next_next_week.append(temp+1).append(". 周").append(get_day_str(getDay(class_date.get(today_class.get(temp))))).append(" ").append(get_calss_info(today_class.get(temp))).append("\n\n");
                PrintQQ(send_next_next_next_week.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "最近":
                StringBuilder send_nearest = new StringBuilder();
                send_nearest.append(getDateString(calendar,full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                List<Integer> nearest = new ArrayList<>();
                for (int temp : today_class){
                    if(class_date.get(temp).after(calendar)) nearest.add(temp);
                    if(nearest.size()==3) break;
                }
                if(nearest.size()<3){
                    calendar.add(Calendar.DAY_OF_YEAR,1);
                    today_class = get_today_class(calendar);
                    calendar.add(Calendar.DAY_OF_YEAR,-1);
                    for (int temp : today_class){
                        if(class_date.get(temp).after(calendar)) nearest.add(temp);
                        if(nearest.size()==3) break;
                    }
                }
                send_nearest.append("最近的课程如下").append("\n====================\n");
                for (int temp=0;temp<nearest.size();temp++) send_nearest.append(temp+1).append(". 周").append(get_day_str(getDay(class_date.get(nearest.get(temp))))).append(" ").append(getDateString(class_date.get(nearest.get(temp)),hour)).append(" ").append(get_calss_info(nearest.get(temp))).append("\n\n");
                PrintQQ(send_nearest.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "科目":
                StringBuilder send_subjects = new StringBuilder();
                send_subjects.append("本学期科目如下：").append("\n");
                for (String s:new HashSet<>(class_name.values())) send_subjects.append(s).append("\n");
                PrintQQ(send_subjects.toString(), getConfig("QQPrint"), true, qq);
                break;
            case "统计":
                PrintQQ("本学期共有"+(last_avail_number-1)+"节课，"+(getSumSection())+"个小节课程", getConfig("QQPrint"), true, qq);
                break;
            case "check":
                return check(today_class,calendar,qq);
            case "帮助":
                PrintQQ("您可选的查询指令包括：\n最近、今、今日、今天、明、明日、明天、后天、本周、下周、下下周、下下下周、统计、科目、课程全学期查询（如：查+科目名【支持模糊搜索】）、时间查询（如：查周一6-10）", getConfig("QQPrint"), true, qq);
                break;
        }
        return 0;
    }
    private long check(List<Integer> today_class, Calendar calendar, String qq){
        StringBuilder send_info = new StringBuilder();
        for (int i=0;i<today_class.size();i++) {
            int a = today_class.get(i);
            long left = class_end_date.get(a).getTimeInMillis() - calendar.getTimeInMillis();
            long before = class_date.get(a).getTimeInMillis() - calendar.getTimeInMillis();
            if(calendar.get(Calendar.HOUR_OF_DAY)==22){
                if(calendar.get(Calendar.MINUTE)<20){
                    if(!broadcast) {
                        PrintQQ("早睡早起身体好", getConfig("QQPrint"), true, qq);
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) { }
                        func("明天", qq);
                        broadcast = true;
                        return 60000*21;
                    }
                }
            }else{
                broadcast = false;
            }
//            PrintConsole(get_calss_info(a));
//            PrintConsole(getDateString(calendar, full));
//            PrintConsole(class_end_date.get(a).getTimeInMillis());
//            PrintConsole(calendar.getTimeInMillis());
//            PrintConsole(left/60000);
            if (mode==XI && left>0 && left / 60000 < 10) {
                send_info.append(getDateString(calendar, full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    今日共有").append(today_class.size()).append("门课程").append("\n====================\n");
                send_info.append(class_name.get(a)).append(" 还有").append(left / 60000).append("分钟就要下课了").append("\n");
                if(i!=today_class.size()-1) send_info.append("下一节课是").append(get_calss_info(today_class.get(i+1)));
                else send_info.append("今日课程即将全部上完，快去快乐吧233");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                return 10*60000;
            }
            if(mode==XI && before>0 && begin_end.get(a).get(0)==6 && before / 60000 < 45){
                List<Integer> nearest = new ArrayList<>();
                for (int temp : today_class){ if(class_date.get(temp).after(calendar)) nearest.add(temp); }
                send_info.append(getDateString(calendar, full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    下午和晚上共有").append(nearest.size()).append("门课程").append("\n====================\n");
                send_info.append("** 还有").append(before / 60000).append("分钟就要下午上课了").append("\n");
                for (int temp=0;temp<nearest.size();temp++) send_info.append(temp+1).append(". ").append(get_calss_info(nearest.get(temp))).append("\n\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                return before+60000*5;
            }else if(mode==NK && before>0 && begin_end.get(a).get(0)==5 && before / 60000 < 45){
                List<Integer> nearest = new ArrayList<>();
                for (int temp : today_class){ if(class_date.get(temp).after(calendar)) nearest.add(temp); }
                send_info.append(getDateString(calendar, full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    下午和晚上共有").append(nearest.size()).append("门课程").append("\n====================\n");
                send_info.append("** 还有").append(before / 60000).append("分钟就要下午上课了").append("\n");
                for (int temp=0;temp<nearest.size();temp++) send_info.append(temp+1).append(". ").append(get_calss_info(nearest.get(temp))).append("\n\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                return before+60000*5;
            }else if(mode==NK && before>0 && begin_end.get(a).get(0)==9 && before / 60000 < 45){
                List<Integer> nearest = new ArrayList<>();
                for (int temp : today_class){ if(class_date.get(temp).after(calendar)) nearest.add(temp); }
                send_info.append(getDateString(calendar, full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    晚上共有").append(nearest.size()).append("门课程").append("\n====================\n");
                send_info.append("** 还有").append(before / 60000).append("分钟就要晚上上课了").append("\n");
                for (int temp=0;temp<nearest.size();temp++) send_info.append(temp+1).append(". ").append(get_calss_info(nearest.get(temp))).append("\n\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                return before+60000*5;
            } else if(i==0 && before > 0 && before / 60000 < 45){
                List<Integer> nearest = new ArrayList<>();
                for (int temp : today_class){ if(class_date.get(temp).after(calendar)) nearest.add(temp); }
                send_info.append(getDateString(calendar, full)).append("  星期").append(get_day_str(getDay(calendar))).append("\n");
                send_info.append("上课第").append(getWeek(calendar)).append("周").append("    今日共有").append(nearest.size()).append("门课程").append("\n====================\n");
                send_info.append("** 还有").append(before / 60000).append("分钟就要开始上课了").append("\n");
                for (int temp=0;temp<nearest.size();temp++) send_info.append(temp+1).append(". ").append(get_calss_info(nearest.get(temp))).append("\n\n");
                PrintQQ(send_info.toString(), getConfig("QQPrint"), true, qq);
                return before+60000*5;
            }
        }
        return 3*60000;
    }
    public int getWeek(Calendar c){
        return 1 + c.get(Calendar.WEEK_OF_YEAR)-start_day.get(Calendar.WEEK_OF_YEAR);
    }
    public int getDay(Calendar c){
        int day =  c.get(Calendar.DAY_OF_WEEK) -1 ;
        if(day == 0) day = 7;
        return day;
    }
    public String getDateString(Calendar c,SimpleDateFormat s){
        Date d = new Date();
        d.setTime(c.getTimeInMillis());
        return s.format(d);
    }
    private String get_calss_info(int class_index){
        return ""+begin_end.get(class_index).get(0)+"-"+begin_end.get(class_index).get(begin_end.get(class_index).size()-1)+" "+class_name.get(class_index)+" "+class_where.get(class_index)+"    "+"\n"+"班级："+class_with_who.get(class_index) + " " +getDateString(class_date.get(class_index),minmin)+(class_addition_info.get(class_index).length()>0?"\n提示："+class_addition_info.get(class_index):"");
    }
    private String get_timed_class_info(int class_index){
        return "第"+getWeek(class_date.get(class_index))+"周 "+begin_end.get(class_index).get(0)+"-"+begin_end.get(class_index).get(begin_end.get(class_index).size()-1)+" "+class_name.get(class_index)+"  "+class_where.get(class_index);
    }
    private String get_subject_class_info(int class_index,boolean mini){
        if(!mini) return "第"+getWeek(class_date.get(class_index))+"周周"+get_day_str(getDay(class_date.get(class_index)))+begin_end.get(class_index).get(0)+"-"+begin_end.get(class_index).get(begin_end.get(class_index).size()-1)+" "+class_name.get(class_index)+"  "+class_where.get(class_index);
        else return "第"+getWeek(class_date.get(class_index))+"周周"+get_day_str(getDay(class_date.get(class_index)))+begin_end.get(class_index).get(0)+"-"+begin_end.get(class_index).get(begin_end.get(class_index).size()-1)+"  "+class_where.get(class_index);
    }
//    +" "+getClassTimeHourAndMinuteStartAndEnd(begin_end.get(class_index)).get(0)+":"+getClassTimeHourAndMinuteStartAndEnd(begin_end.get(class_index)).get(1)+"-"+getClassTimeHourAndMinuteStartAndEnd(begin_end.get(class_index)).get(2)+":"+getClassTimeHourAndMinuteStartAndEnd(begin_end.get(class_index)).get(3)
    private void sortAll(){
        getMax();
        int class_index = 1;
        for (int temp_week = 1;temp_week<=week_max;temp_week++){
            for(int temp_day = 1;temp_day<=7;temp_day++){
                for (int temp_end = 1;temp_end<=end_max;temp_end++){
                    for (Map.Entry<Integer,List<Integer>> entry:week_info.entrySet()){
                        if(entry.getValue().get(0).equals(temp_week)&&entry.getValue().get(1).equals(temp_day)&&begin_end.get(entry.getKey()).get(0).equals(temp_end)){
                            class_turn.put(class_index,entry.getKey());
                            class_index++;
                        }
                    }
                }
            }
        }
//        for (int i = 60;i<last_avail_number;i++){
//            class_index = class_turn.get(i);
//            PrintConsole("第"+week_info.get(class_index).get(0)+"周 星期"+get_day_str(week_info.get(class_index).get(1))+" 第"+begin_end.get(class_index).get(0)+"到"+begin_end.get(class_index).get(begin_end.get(class_index).size()-1)+"节   即"+getDateString(class_date.get(class_index),full));
//            PrintConsole("总第"+i+"节 课程名："+class_name.get(class_index)+" 上课地点："+class_where.get(class_index));
//            PrintConsole("上课班级："+class_with_who.get(class_index)+" 其他信息："+class_addition_info.get(class_index));
//        }
    }
    private void getMax(){
        for(List<Integer> c12:week_info.values()){
            if(c12.get(0)>week_max) week_max=c12.get(0);
            if(c12.get(1)>day_max) day_max=c12.get(1);
        }
        for(List<Integer> c12:begin_end.values()){
            if(c12.get(c12.size()-1)>end_max) end_max=c12.get(1);
        }
    }
    private List<Integer> each_class_weeks(String origin){
        List<Integer> datas = new ArrayList<>();
        for(String sub_info : origin.split(",")){
            if(sub_info.contains("-")){
                int gap = 1;
                if(sub_info.contains("单")||sub_info.contains("双")) gap = 2;
                String[] first_last = sub_info.split("-");
                int first = Integer.parseInt(first_last[0]);
                int last = Integer.parseInt(first_last[1].split("周")[0]);
                for(;first<=last;first+=gap) datas.add(first);
            }else{
                datas.add(Integer.parseInt(sub_info.split("周")[0]));
            }
        }
        return datas;
    }
    private List<Integer> get_begin_end(String begin,String end){
        List<Integer> datas = new ArrayList<>();
        int begin_int = Integer.parseInt(begin.replace("第","").replace("节",""));
        int end_int = Integer.parseInt(end.replace("第","").replace("节",""));
        for (;begin_int<=end_int;begin_int++){
            datas.add(begin_int);
        }
        return datas;
    }
    private int get_day(String origin){
        if(origin.contains("一")) return 1;
        if(origin.contains("二")) return 2;
        if(origin.contains("三")) return 3;
        if(origin.contains("四")) return 4;
        if(origin.contains("五")) return 5;
        if(origin.contains("六")) return 6;
        if(origin.contains("日")) return 7;
        return 0;
    }
    private int get_inner_day(String origin){
        if(origin.contains("一")) return Calendar.MONDAY;
        if(origin.contains("二")) return Calendar.TUESDAY;
        if(origin.contains("三")) return Calendar.WEDNESDAY;
        if(origin.contains("四")) return Calendar.THURSDAY;
        if(origin.contains("五")) return Calendar.FRIDAY;
        if(origin.contains("六")) return Calendar.SATURDAY;
        if(origin.contains("日")) return Calendar.SUNDAY;
        return 0;
    }
    private String get_day_str(int which){
        switch (which){
            case 1: return "一";
            case 2: return "二";
            case 3: return "三";
            case 4: return "四";
            case 5: return "五";
            case 6: return "六";
            case 7: return "日";
        }
        return "出错";
    }
    private List<Integer> get_today_class(Calendar c){
        List<Integer> data = new ArrayList<>();
        for (int a :class_turn.values()){
            if(class_date.get(a).get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)){
                data.add(a);
            }
        }
        return data;
    }
    private List<Integer> getClassTimeHourAndMinuteStartAndEnd(List<Integer> l){
        int start = l.get(0);
        int end = l.get(l.size()-1);
        List<Integer> data = new ArrayList<>();

        if(mode==NK){
            if(l.get(0)==1){
                data.add(8);
                data.add(0);
                data.add(9);
                data.add(50);
            }
            if(l.get(0)==3){
                data.add(10);
                data.add(20);
                data.add(12);
                data.add(10);
            }
            if(l.get(0)==5){
                data.add(14);
                data.add(0);
                data.add(15);
                data.add(50);
            }
            if(l.get(0)==7){
                data.add(16);
                data.add(20);
                data.add(18);
                data.add(10);
            }
            if(l.get(0)==9){
                data.add(19);
                data.add(0);
                data.add(20);
                data.add(50);
            }
            return data;
        }

        int class_time = (end - start+1) * 40 + (end - start) * 10;
        if(start<6) {
            if(l.size()>3) class_time+=10; //需要测试是否补齐
            int before_class_time = (start-1)*40+(Math.max(start-2,0))*10;
            if (start > 2) before_class_time += 20;
            data.add(8 + before_class_time / 60);
            data.add(before_class_time % 60);
            data.add(8 + (before_class_time + class_time) / 60);
            data.add((before_class_time + class_time) % 60);
        }else if(start<11){
            int before_class_time = (start-6)*40+(Math.max(start-6,0))*10;
            data.add(14 + before_class_time / 60);
            data.add(before_class_time % 60);
            data.add(14 + (before_class_time + class_time) / 60);
            data.add((before_class_time + class_time) % 60);
        }else{
            int before_class_time = (start-11)*40+(Math.max(start-11,0))*10+30;
            data.add(19 + before_class_time / 60);
            data.add(before_class_time % 60);
            data.add(19 + (before_class_time + class_time) / 60);
            data.add((before_class_time + class_time) % 60);
        }
        return data;
    }
    private int getSumSection(){
        int sum = 0;
        for (List<Integer> data : begin_end.values()){
            sum+=data.size();
        }
        return sum;
    }
    private Calendar giveCalendar(Date d){
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }
    private Calendar getChangeCalendar(Calendar c){
        for(Calendar a : change_class_date.keySet()){
            if(c.get(Calendar.DAY_OF_YEAR)==a.get(Calendar.DAY_OF_YEAR)) return change_class_date.get(a);
        }
        return c;
    }
}
