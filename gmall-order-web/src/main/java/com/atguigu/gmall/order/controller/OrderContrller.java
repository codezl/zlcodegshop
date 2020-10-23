package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderContrller {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, String orderNo, HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //检查交易吗
        String success = orderService.checkTradeCode(memberId, tradeCode);


        if (success.equals("success")) {
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            //订单对象
            OmsOrder omsOrder = new OmsOrder();
            //自动过期时间
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            //打折字段未填写,直接写死8折
//            BigDecimal bigDecimal10 = new BigDecimal("10");
//            BigDecimal bigDecimal8 = new BigDecimal("8");
            omsOrder.setDiscountAmount(omsOrder.getTotalAmount());
            //omsOrder.setFreightAmount();//运费支付之后生成
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            ///订单备注可以去完成
            omsOrder.setNote(orderNo);
            String outTradeNo = "zl";
            //订单号
            outTradeNo = outTradeNo+System.currentTimeMillis();//拼接时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());//拼接时间字符串
            omsOrder.setOrderSn(outTradeNo);
            omsOrder.setPayAmount(totalAmount);
            //订单类型
            omsOrder.setOrderType(1);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            //当前日期加一天，配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            //类型和状态
            omsOrder.setSourceType(0);
            omsOrder.setStatus("0");
            omsOrder.setTotalAmount(totalAmount);

            //还有积分，折扣，等功能未写完



            //根据用户id获得购买商品和总价格
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1"));{
                //订单详情
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                //检验价格
                boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                if (b==false){
                    ModelAndView mv = new ModelAndView("tradeFail");
                    return mv;
                }
                //检验库存,调用库存系统
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductName(omsCartItem.getProductName());


                omsOrderItem.setOrderSn(outTradeNo);//订单号，和其他系统交互使用，防止重复
                omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                //商品吗字段没写
                omsOrderItem.setProductSkuCode("202020202020");
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setProductId(omsCartItem.getProductId());
                omsOrderItem.setProductSn("仓库编号：2020");

                //还可以加入打折的属性

                omsOrderItems.add(omsOrderItem);
            }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);





            //将订单写入数据库
            //删除购物车商品
            orderService.saveOrder(omsOrder);

            //重定向支付系统
            ModelAndView mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",totalAmount);

            return mv;
        } else {
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {
        //将数据库的值传到ToTrade页面，以下功能都将使用
        //qiangzhuan 避免空值不要使用toString
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //收获地址
        List<UmsMemberReceiveAddress> userAddressList = userService.getReceiveAddressByMemberId(memberId);

        System.out.println(userAddressList);
        //将购物车集合妆化结算清单
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            //循环一个购物车对象，就封装一个商品详情
            if (omsCartItem.getIsChecked().equals("1")) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                //增加属性，功能还未写全
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                BigDecimal relamount = omsCartItem.getQuantity().multiply(omsCartItem.getPrice());
                omsOrderItem.setRealAmount(relamount);
                omsOrderItems.add(omsOrderItem);
            }

        }

        modelMap.put("omsOrderItems", omsOrderItems);
        modelMap.put("userAddressList", userAddressList);
        //加入购物车的总金额
        modelMap.put("totalAmount", getTotalAmount(omsCartItems));


        modelMap.put("nickname",nickname);
        //生成交易吗，提交时校验
        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }


    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            if (omsCartItem.getIsChecked().equals("1")) {
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }
}


