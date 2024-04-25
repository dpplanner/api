package com.dp.dplanner;

import com.dp.dplanner.adapter.bo.ClubMemberBO;
import com.dp.dplanner.adapter.dto.ClubMemberDto;
import com.dp.dplanner.util.DefaultConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DefaultConverterTest {

    @Test
    public void defaultConverterTest() throws Exception
    {

        var dto = ClubMemberDto.Create.builder()
                .info("info")
                .name("name")
                .clubId(1L)
                .build();

        var bo = DefaultConverter.convert(dto, ClubMemberBO.class);

        System.out.println(bo);

    }
}
