package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import javassist.bytecode.stackmap.BasicBlock;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.session.SessionProperties;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.security.Key;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        // 插入skuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);



    }


    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId){

        // sku商品对象
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        //skutupian
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImages);

        return skuInfo;

    }


    @Override
    public PmsSkuInfo getSkuById(String skuId,String ip) {
       System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"商品详情请求");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        //lianjie huancun
        Jedis jedis = redisUtil.getJedis();

        //chaxun huancun
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);


        //判断有无缓存，决定是否查询mysql数据库
        if (StringUtils.isNotBlank(skuJson)){
            System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"缓存的商品详情");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            //没有缓存
            System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"没有缓存需要信息，申请分布式锁："+"sku:"+skuId+":lock");

            String token = UUID.randomUUID().toString();

            //设置分布式锁
            String OK = jedis.set("sku:"+skuId+":lock","token","nx","px",10*1000);//设置过期时间



            if(StringUtils.isNotBlank(OK)&&OK.equals("OK")){
                    //设置成功，可以在10秒内访问数据库
                    System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"可以在10秒内访问数据库："+"sku:"+skuId+":lock");

                   //取得数据
                    pmsSkuInfo =  getSkuByIdFromDb(skuId);

                    if(pmsSkuInfo!=null){

                        //sql查询结果存入redis
                        jedis.set("sku:"+skuId+":info", JSON.toJSONString(pmsSkuInfo));
                    }else {
                        //防止huncun穿透。设置数据null或孔子符,并设置存在时间
                        jedis.setex("sku:" + skuId + ":info", 1000, JSON.toJSONString(""));

                    }

                         //访问成功后shi放 redis锁
                    System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"归还锁："+"sku:"+skuId+":lock");
                       String lockToken = jedis.get("sku:"+skuId+":lock");
                       if(StringUtils.isNotBlank(lockToken)&&lockToken.equals(token)){
                          // jedis.eval("luna");//高并发下可用Luna脚本，查询到值同时删除，避免意外
                           jedis.del("sku:"+skuId+":lock");//确保删除自己的锁
                    }



            }else {
                //shezhi失败，设置睡眠时间后，重新执行,zixuan
                System.out.println("ip为"+ip+"的线程:"+Thread.currentThread().getName()+"没拿到锁，开始zi旋："+"sku:"+skuId+":lock");
                try {
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
               return getSkuById(skuId,ip); //需要加上return，不会创建新的线程
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(select);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {
        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal price = pmsSkuInfo1.getPrice();

        if (price.compareTo(productPrice)==0){
            b = true;
        }

        return b;
    }


    //
//    @Override
//    public PmsSkuInfo getSkuById(String skuId) {
//        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
//        pmsSkuInfo.setId(skuId);
//
//        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
//        return skuInfo;
//    }

//    @Override
//    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
//        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
//
//        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
//            String skuId = pmsSkuInfo.getId();
//
//            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
//            pmsSkuAttrValue.setSkuId(skuId);
//            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
//
//            pmsSkuInfo.setSkuAttrValueList(select);
//        }
//        return pmsSkuInfos;
//    }



}
