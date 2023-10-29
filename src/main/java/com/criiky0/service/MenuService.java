package com.criiky0.service;

import com.criiky0.pojo.Menu;
import com.baomidou.mybatisplus.extension.service.IService;
import com.criiky0.pojo.dto.MenuDTO;
import com.criiky0.utils.Result;
import com.criiky0.utils.ResultCodeEnum;

import java.util.HashMap;
import java.util.List;

/**
 * @author criiky0
 * @description 针对表【menu】的数据库操作Service
 * @createDate 2023-10-26 14:24:12
 */
public interface MenuService extends IService<Menu> {

    Result<HashMap<String, Menu>> addMenu(Menu menu);

    Result<ResultCodeEnum> deleteMenu(Long menuId);

    List<MenuDTO> findSubMenu(MenuDTO rootMenu);

    Result<HashMap<String, List<MenuDTO>>> getMenuOfCriiky0();
}
