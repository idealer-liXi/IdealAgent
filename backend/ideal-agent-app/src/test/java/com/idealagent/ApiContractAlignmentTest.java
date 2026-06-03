package com.idealagent;

import com.idealagent.api.IAiConfigApi;
import com.idealagent.api.IAiApi;
import com.idealagent.api.IAuthApi;
import com.idealagent.api.IUserApi;
import com.idealagent.trigger.controller.AiConfigController;
import com.idealagent.trigger.controller.AiController;
import com.idealagent.trigger.controller.AuthController;
import com.idealagent.trigger.controller.UserController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractAlignmentTest {
    @Test
    void controllersImplementMiniAgentStyleApiContracts() {
        assertThat(AuthController.class).isAssignableTo(IAuthApi.class);
        assertThat(UserController.class).isAssignableTo(IUserApi.class);
        assertThat(AiController.class).isAssignableTo(IAiApi.class);
        assertThat(AiConfigController.class).isAssignableTo(IAiConfigApi.class);
    }
}
