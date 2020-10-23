package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsCartItemMapper omsCartItemMapper;
    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItem1 = omsCartItemMapper.selectOne(omsCartItem);
        return omsCartItem1;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        System.out.println(omsCartItem.getQuantity());
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())) {
            omsCartItemMapper.insertSelective(omsCartItem);//避免添加空值
        }
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        Example e = new Example((OmsCartItem.class));
        e.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());

        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb,e);
    }

    @Override
    public void flushCartCache(String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

        //同步到redis
        Jedis jedis = redisUtil.getJedis();

        Map<String,String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            //计算单间商品总价
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }


        jedis.del("user:"+memberId+":cart");
        jedis.hmset("user:"+memberId+":cart",map);


        jedis.close();


    }

    @Override
    public List<OmsCartItem> cartList(String userId) {
        flushCartCache(userId);
        Jedis jedis = null;
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try{
            jedis = redisUtil.getJedis();
            List<String> hvals = jedis.hvals("user:" + userId + ":cart");

            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval,OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
        }catch (Exception e){
            //处理异常，记录日志
            e.printStackTrace();
            //String message = e.getMessage();
            //logService.addErrlog(message);
            return null;
        }finally {
            jedis.close();
        }

        if (omsCartItems==null){
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(userId);
            omsCartItems = omsCartItemMapper.select(omsCartItem);
            return omsCartItems;
        }

        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {

        Example e = new Example(OmsCartItem.class);

        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());

        omsCartItemMapper.updateByExampleSelective(omsCartItem,e);

        //缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }

    @Override
    public void delcart(String skuId, String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);

        omsCartItemMapper.delete(omsCartItem);


        //缓存同步
        flushCartCache(memberId);
    }

    @Override
    public void delchk(String memberId, String ischk) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setIsChecked(ischk);
        omsCartItemMapper.delete(omsCartItem);


        //缓存同步
        flushCartCache(memberId);
    }

    @Override
    public void chkall(OmsCartItem omsCartItem) {
        Example e = new Example(OmsCartItem.class);

        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId());

        omsCartItemMapper.updateByExampleSelective(omsCartItem,e);

        //缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }
}
