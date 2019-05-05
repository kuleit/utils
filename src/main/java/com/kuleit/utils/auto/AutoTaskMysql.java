package com.kuleit.utils.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Description: AutoTaskMysql
 * @Author: Wx
 * @date: 2019/5/5 18:07
 */
public class AutoTaskMysql {

    /**
     * 保存的路径及文件的名称
     *
     * @ linux: "/soft/backup/lideerp";
     * @ windows: D://backup//
     */
    public static String PATH = "/soft/backup/lideerp";

    /**
     * 文件名
     */
    public static String FINE_NAME = "";

    /**
     * SqlUrl
     * linux: //soft//tomcat//apache-tomcat-8.5.32//bin//mysqldump -h 120.78.153.87 -ulideerp_remote -p97hidyx6hW=HQEoohHY&{)8e --databases test
     * windows: D://work//bins//bins//mysql//mysql5/bin/mysqldump -h 127.0.0.1 -uroot -p123456 --databases lideerp_dev
     */
    public static String SQL_PATH = "/soft/mysql/mysql-5.7.18-linux-glibc2.5-x86_64/bin/mysqldump --defaults-extra-file=/etc/my.cnf test";

    /**
     * 日志文件
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @return void
     * @Description: //TODO 启动执行
     * @author: Wx
     * @date: 2019/5/5 9:59
     * @Param []
     */
    public static void main(String[] args) {
        // 开始备份
        startBackup(SQL_PATH);
        System.out.println("文件:" + FINE_NAME + " —> 保存结束..");
        // 路径文件名
        String srcFileName = PATH + FINE_NAME + ".sql";
        // 压缩后的路径名称
        String compressFineName = PATH + FINE_NAME + ".zip";
        // 开始压缩
        compress(srcFileName, compressFineName);
        System.out.println("文件:" + FINE_NAME + " —> 压缩结束..");
        // 开始发送邮件
        sendEmail();
        System.out.println("文件:" + FINE_NAME + " —> 发送结束..");
    }

    /**
     * @return
     * @Description: //TODO 开始备份数据
     * @author: Wx
     * @date: 2019/5/5 10:07
     * @Param @sqlurl Mysql安装路径
     */
    public static void startBackup(String sqlurl) {
        try {
            Runtime rt = Runtime.getRuntime();
            //执行cmd命令
            Process child = rt.exec(sqlurl);
            // 设置导出编码为utf-8 控制台的输出信息作为输入流
            InputStream in = child.getInputStream();
            InputStreamReader xx = new InputStreamReader(in, "utf-8");
            // 设置输出流编码为utf-8。这里必须是utf-8，否则从流中读入的是乱码
            String inStr;
            StringBuffer sb = new StringBuffer("");
            String outStr;
            // 组合控制台输出信息字符串
            BufferedReader br = new BufferedReader(xx);
            while ((inStr = br.readLine()) != null) {
                sb.append(inStr + "\r\n");
            }
            outStr = sb.toString();
            // 生成时间并导出文件到自定义目录
            FileOutputStream fout = new FileOutputStream(setBackupSqlFileName());
            OutputStreamWriter writer = new OutputStreamWriter(fout, "utf-8");
            writer.write(outStr);
            writer.flush();
            in.close();
            xx.close();
            br.close();
            writer.close();
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return java.lang.String
     * @Description: //TODO 生成时间并导出到目录
     * @author: Wx
     * @date: 2019/5/5 10:42
     * @Param []
     */
    private static String setBackupSqlFileName() {
        // 生成一个文件名
        FINE_NAME = String.valueOf(System.currentTimeMillis());
        // 保存路径
        String savePath = PATH + FINE_NAME + ".sql";
        return savePath;
    }

    /**
     * @return void
     * @Description: //TODO 压缩文件
     * @author: Wx
     * @date: 2019/5/5 10:52
     * @Param [srcFilePath 源文件, destFilePath 压缩目的路径]
     */
    private static void compress(String srcFilePath, String destFilePath) {
        // 创建一个源文件
        File src = new File(srcFilePath);
        // 校验文件是否存在
        if (!src.exists()) {
            throw new RuntimeException(srcFilePath + "不存在");
        }

        // 创建一个压缩文件
        File zipFile = new File(destFilePath);

        try {
            // 开始压缩
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            String baseDir = "";
            compressbyType(src, zos, baseDir);
            zos.close();
        } catch (Exception e) {

        }
    }

    /**
     * @return void
     * @Description: //TODO 按照原路径的类型就行压缩。文件路径直接把文件压缩
     * @author: Wx
     * @date: 2019/5/5 10:55
     * @Param [src, zos, baseDir]
     */
    private static void compressbyType(File src, ZipOutputStream zos, String baseDir) {
        if (!src.exists()) {
            // 文件不存在
            return;
        }
        //  判断文件是否是文件，如果是文件调用compressFile方法,如果是路径，则调用compressDir方法
        if (src.isFile()) {
            // src是文件，调用此方法
            compressFile(src, zos, baseDir);
        } else {
            // src是文件夹，调用此方法
            compressDir(src, zos, baseDir);
        }
    }


    /**
     * @return void
     * @Description: //TODO 文件压缩
     * @author: Wx
     * @date: 2019/5/5 10:58
     * @Param [src, zos, baseDir]
     */
    private static void compressFile(File src, ZipOutputStream zos, String baseDir) {
        if (!src.exists()) {
            // 文件是否存在
            return;
        }
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
            ZipEntry entry = new ZipEntry(baseDir + src.getName());
            zos.putNextEntry(entry);
            int count;
            byte[] buf = new byte[1024];
            while ((count = bis.read(buf)) != -1) {
                zos.write(buf, 0, count);
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return void
     * @Description: //TODO 目录压缩
     * @author: Wx
     * @date: 2019/5/5 10:58
     * @Param [src, zos, baseDir]
     */
    private static void compressDir(File src, ZipOutputStream zos, String baseDir) {
        if (!src.exists()) {
            return;
        }
        File[] files = src.listFiles();

        if (files.length == 0) {
            try {
                zos.putNextEntry(new ZipEntry(baseDir + src.getName() + File.separator));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (File file : files) {
            compressbyType(file, zos, baseDir + src.getName() + File.separator);
        }
    }

    /**
     * @return void
     * @Description: //TODO 邮件发送
     * @author: Wx
     * @date: 2019/5/5 11:26
     * @Param []
     */
    private static void sendEmail() {
        // 是否校验
        boolean enable = true;
        // 是否打印控制台消息
        boolean debug = true;
        // 发件人地址
        String sendAddress = "mali@dreamblue.net.cn";
        // 邮件接收者地址
        String sendToAddress = "mali@dreamblue.net.cn";
        // 邮件主题
        String subject = "利德ERP能源管理平台";
        // 邮件内容
        String context = "尊敬的用户您好,这是《利德ERP能源管理平台》[ " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ] 前所有信息数据,请保存好文件,丢失泄露概不负责. \n ";
        // 文本路径
        String path = PATH + FINE_NAME + ".zip";
        // 自己的邮件帐号
        String owner = "mali@dreamblue.net.cn";
        // 授权码
        String ownerStmp = "W[(KBuvm*w34zuVgs88?";
        // 配置对象
        Properties properties = new Properties();
        // 连接协议
        properties.put("mail.transport.protocol", "smtp");
        // 主机名 | qq:smtp.qq.com
        properties.put("mail.smtp.host", "smtp.exmail.qq.com");
        // 端口号
        properties.put("mail.smtp.port", 465);
        // 认证
        properties.put("mail.smtp.auth", "true");
        // 设置是否使用ssl安全连接 ---一般都使用
        properties.put("mail.smtp.ssl.enable", enable);
        // 设置是否显示debug信息 true 会在控制台显示相关信息
        properties.put("mail.debug", debug);

        try {
            // 得到回话对象
            Session session = Session.getInstance(properties);
            // 获取邮件对象
            Message message = new MimeMessage(session);
            // 设置发件人邮箱地址
            message.setFrom(new InternetAddress(owner));
            // 设置收件人邮箱地址
            message.setRecipients(Message.RecipientType.TO, new InternetAddress[]{new InternetAddress(sendToAddress)});
            // 设置邮件标题
            message.setSubject(subject);
            // 创建消息部分
            BodyPart messageBodyPart = new MimeBodyPart();
            // 消息
            messageBodyPart.setText(context);
            // 创建多重消息
            Multipart multipart = new MimeMultipart();
            // 设置文本消息部分
            multipart.addBodyPart(messageBodyPart);
            // 附件部分
            messageBodyPart = new MimeBodyPart();
            // 设置要发送附件的文件路径
            DataSource source = new FileDataSource(path);
            messageBodyPart.setDataHandler(new DataHandler(source));
            //设置当前发送邮件时间
            message.setSentDate(new Date());
            // 处理附件名称中文（附带文件路径）乱码问题
            messageBodyPart.setFileName(MimeUtility.encodeText(path));
            multipart.addBodyPart(messageBodyPart);
            // 发送完整消息
            message.setContent(multipart);
            // 得到邮差对象
            Transport transport = session.getTransport();
            // 连接自己的邮箱账户
            transport.connect(owner, ownerStmp);// 密码为QQ邮箱开通的stmp服务后得到的客户端授权码
            // 发送邮件
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            // 清空备份目录
            deleteBackupDir(PATH);
        } catch (Exception e) {
            System.out.println("信息发送失败..");
            e.printStackTrace();
        }
    }

    /**
     * @return void
     * @Description: //TODO  备份后清空目录
     * @author: Wx
     * @date: 2019/5/5 15:15
     * @Param []
     */
    private static void deleteBackupDir(String path) {
        File file = new File(path);
        // 判断是否待删除目录是否存在
        if (!file.exists()) {
            return;
        }
        // 取得当前目录下所有文件和文件夹
        String[] content = file.list();
        try {
            for (String name : content) {
                File temp = new File(path, name);
                // 判断是否是目录
                if (temp.isDirectory()) {
                    //递归调用，删除目录里的内容
                    deleteBackupDir(temp.getAbsolutePath());
                    temp.delete();//删除空目录
                } else {
                    if (!temp.delete()) {//直接删除文件
                        System.err.println("Failed to delete " + name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
